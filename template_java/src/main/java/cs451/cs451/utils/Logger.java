package cs451.utils;

public class Logger {
    private String path;

    public Logger(String path) {
        this.path = path;
    }

    public void logBroadcast(int seqNum) {
        System.out.println("---------------------- b " + seqNum);
    }

    public void logSend(int recipientId, int seqNum) {
        System.out.println("---------------------- s " + recipientId + " " + seqNum);
    }

    public synchronized void logDeliver(int senderId, int seqNum) {
        System.out.println("---------------------- d " + senderId + " " + seqNum);
    }
}
