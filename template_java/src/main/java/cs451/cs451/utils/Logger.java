package cs451.utils;

import cs451.Main;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Logger extends Thread{
    private FileWriter writer;
    private List<String> logEvents;
    private boolean interrupted;
    private int deliveredMsgs;
    private int ownDeliveredMsgs;
    private int broadcastMsgs;
    private int numHosts;
    private int numMsgs;
    private int thisHostId;

    public static final int GET = 0;
    public static final int INC = 1;

    public Logger(String path, int numHosts, int numMsgs, int thisHostId) {
        writer = null;
        try {
            writer = new FileWriter(path);
        } catch (IOException e) {
            System.err.println("Error occurred opening output file: " + path);
        }
        logEvents = Collections.synchronizedList(new ArrayList<>());
        interrupted = false;
        deliveredMsgs = 0;
        ownDeliveredMsgs = 0;
        broadcastMsgs = 0;
        this.numHosts = numHosts;
        this.numMsgs = numMsgs;
        this.thisHostId = thisHostId;

    }

    public void logBroadcast(int seqNum) {
        //System.out.println("---------------------- b " + seqNum);
        if(!accessInterrupted(false)) {
            accessBroadcastMsgs(INC);
            logEvents.add("b " + seqNum);
        }
    }

    public void logSend(int recipientId, int seqNum) {
        //System.out.println("---------------------- s " + recipientId + " " + seqNum);
        if(!accessInterrupted(false)) {
            logEvents.add("s " + recipientId + " " + seqNum);
        }
    }

    /*
    logEvents.add("d " + senderId + " " + seqNum);
            int tempNumDel = accessDeliveredMsgs(INC);
            int tempNumBroad = accessBroadcastMsgs(GET);
            System.out.println("(" + thisHostId + ") " + tempNumDel);

            int tempOwnDel = -1;
            if(senderId == thisHostId) {
                tempOwnDel = accessOwnDeliveredMsgs(INC);
            }
            if(tempOwnDel != -1 && tempOwnDel == tempNumBroad) {
                synchronized (this) {
                    this.notify();
                }
            }
     */

    public synchronized void logDeliver(int senderId, int seqNum) {
        //System.out.println("---------------------- d " + senderId + " " + seqNum);
        if(!accessInterrupted(false)) {
            /*
            logEvents.add("d " + senderId + " " + seqNum);
            int tempNumDel = accessDeliveredMsgs(INC);
            int tempNumBroad = accessBroadcastMsgs(GET);
            System.out.println("(" + thisHostId + ") " + tempNumDel);

            int tempOwnDel = -1;
            if(senderId == thisHostId) {
                tempOwnDel = accessOwnDeliveredMsgs(INC);
            }
            if(tempNumDel > (tempNumBroad+1)*Math.floor(((double)numHosts)/2.0)) {
                synchronized (this) {
                    this.notify();
                }
            }
            */
            logEvents.add("d " + senderId + " " + seqNum);
            int tempNumDel = accessDeliveredMsgs(INC);
            int tempNumBroad = accessBroadcastMsgs(GET);
            System.out.println("(" + thisHostId + ") " + tempNumDel);

            int tempOwnDel = -1;
            if(senderId == thisHostId) {
                tempOwnDel = accessOwnDeliveredMsgs(INC);
            }
            if(tempOwnDel != -1 && tempOwnDel == tempNumBroad) {
                synchronized (this) {
                    this.notify();
                }
            }

            if(tempNumDel == numHosts*numMsgs) {
                System.out.println(thisHostId + " IS DONE!");
            }
        }
    }

    public void logToOutFile() {
        if(writer != null) {
            try {
                for (String event : logEvents) {
                    writer.write(event + "\n");
                    //System.out.println(event);
                }
                writer.close();
            }
            catch(IOException e) {
                System.err.println("Error occurred print log events to output file");
            }
        }
    }

    public synchronized boolean accessInterrupted(boolean interrupt) {
        if(interrupt) {
            interrupted = true;
        }
        return interrupted;
    }

    public synchronized int accessBroadcastMsgs(int op) {
        switch(op) {
            case GET:
                return broadcastMsgs;
            case INC:
                broadcastMsgs++;
                return broadcastMsgs;
            default:
                return -1;
        }
    }

    public synchronized int accessDeliveredMsgs(int op) {
        switch (op) {
            case GET:
                return deliveredMsgs;
            case INC:
                deliveredMsgs++;
                return deliveredMsgs;
            default:
                return -1;
        }
    }

    public synchronized int accessOwnDeliveredMsgs(int op) {
        switch (op) {
            case GET:
                return ownDeliveredMsgs;
            case INC:
                ownDeliveredMsgs++;
                return ownDeliveredMsgs;
            default:
                return -1;
        }
    }
}
