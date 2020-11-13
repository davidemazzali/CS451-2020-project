package cs451.utils;

import cs451.Main;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;

public class Logger {
    private FileWriter writer;
    private List<String> logEvents;
    private boolean interrupted;
    private long deliveredMsgs;
    private long ownDeliveredMsgs;
    private long broadcastMsgs;
    private int numHosts;
    private long numMsgs;
    private int thisHostId;
    private Semaphore logSem; // to regulate access to logEvents

    public static final int GET = 0;
    public static final int INC = 1;

    // max number of events not written to file yet
    private static final int MAX_SIZE_LOG = 1000;

    public Logger(String path, int numHosts, long numMsgs, int thisHostId) {
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
        logSem = new Semaphore(1, true);

        this.numHosts = numHosts;
        this.numMsgs = numMsgs;
        this.thisHostId = thisHostId;
    }

    public void logBroadcast(long seqNum) {
        // check if the event can be logged or if termination signal has been received
        if(!accessInterrupted(false)) {

            // increment number of broadcast messages
            accessBroadcastMsgs(INC);

            try {
                logSem.acquire();
            } catch (InterruptedException e) {
                System.err.println("Error acquiring log semaphore: " + e.getMessage());
            }

            logEvents.add("b " + seqNum);
            if(logEvents.size() >= MAX_SIZE_LOG) { // if logEvent has reached maximum allowed capacity
                // write to file
                logToOutFile();
            }

            logSem.release();
        }
    }

    // synchronized method because deliveries are asynchronous
    public synchronized void logDeliver(int senderId, long seqNum) {
        // check if the event can be logged or if termination signal has been received
        if(!accessInterrupted(false)) {

            // increment number of delivered messages and get it
            long tempNumDel = accessDeliveredMsgs(INC);

            try {
                logSem.acquire();
            } catch (InterruptedException e) {
                System.err.println("Error acquiring log semaphore: " + e.getMessage());
            }

            logEvents.add("d " + senderId + " " + seqNum);
            if(logEvents.size() >= MAX_SIZE_LOG) { // if logEvent has reached maximum allowed capacity
                // write to file
                logToOutFile();
            }

            logSem.release();

            /*
            if(tempNumDel % 100 == 0) {
                System.out.println("(" + thisHostId + ") " + tempNumDel);
            }
            */

            // get number of broadcast messages
            long tempNumBroad = accessBroadcastMsgs(GET);

            long tempOwnDel = -1;
            if(senderId == thisHostId) {
                // if the message was broadcast my this process,
                // increment the number of its own messages it has delivered and get it
                tempOwnDel = accessOwnDeliveredMsgs(INC);
            }
            if(tempOwnDel != -1 && tempOwnDel == tempNumBroad) {
                synchronized (this) {
                    // if this process has delivered all the messages it has broadcast, tell the main it can continue
                    this.notify();
                }
            }

            /*
            if(tempNumDel == numHosts*numMsgs) {
                System.out.println(thisHostId + " IS DONE!");
            }
            */
        }
    }

    public void logToOutFile() {
        if(writer != null) {
            try {
                // write all events to file
                for (String event : logEvents) {
                    writer.write(event + "\n");
                    //System.out.println(event);
                }

                // garbage collect logEvents
                logEvents = new ArrayList<>();
            }
            catch(IOException e) {
                System.err.println("Error occurred printing log events to output file: " + e.getMessage());
            }
        }
    }

    // thread-safe method for signalling when no new events can be logged
    // use actual parameter true to interrupt to forbid new logs,
    // use actual parameter false to see if logging has been forbidden
    public synchronized boolean accessInterrupted(boolean interrupt) {
        if(interrupt) {
            interrupted = true;
        }
        return interrupted;
    }

    // thread-safe method for accessing broadcastMsgs field
    // allowed operations: get and increment
    public synchronized long accessBroadcastMsgs(int op) {
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

    // thread-safe method for accessing deliveredMsgs field
    // allowed operations: get and increment
    public synchronized long accessDeliveredMsgs(int op) {
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

    // thread-safe method for accessing ownDeliveredMsgs field
    // allowed operations: get and increment
    public synchronized long accessOwnDeliveredMsgs(int op) {
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

    public FileWriter getWriter() {
        return writer;
    }
}