package cs451.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Logger extends Thread{
    private FileWriter writer;
    private ArrayList<String> logEvents;
    private boolean interrupted;

    public Logger(String path) {
        writer = null;
        try {
            writer = new FileWriter(path);
        } catch (IOException e) {
            System.err.println("Error occurred opening output file: " + path);
        }
        logEvents = new ArrayList<>();
        interrupted = false;
    }

    public void logBroadcast(int seqNum) {
        //System.out.println("---------------------- b " + seqNum);
        if(!accessInterrupted(false)) {
            logEvents.add("b " + seqNum);
        }
    }

    public void logSend(int recipientId, int seqNum) {
        //System.out.println("---------------------- s " + recipientId + " " + seqNum);
        if(!accessInterrupted(false)) {
            logEvents.add("s " + recipientId + " " + seqNum);
        }
    }

    public synchronized void logDeliver(int senderId, int seqNum) {
        //System.out.println("---------------------- d " + senderId + " " + seqNum);
        if(!accessInterrupted(false)) {
            logEvents.add("d " + senderId + " " + seqNum);
        }
    }

    public void logToOutFile() {
        if(writer != null) {
            try {
                for (String event : logEvents) {
                    writer.write(event + "\n");
                    System.out.println(event);
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
}
