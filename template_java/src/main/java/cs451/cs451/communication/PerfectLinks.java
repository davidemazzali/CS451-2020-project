package cs451.communication;

import cs451.parser.HostsParser;
import cs451.utils.Logger;

import java.util.*;

public class PerfectLinks {
    private static final long ACK = -1;
    private static final long INIT_TO = 1000;
    private static final float ALPHA = 0.125f;
    private static final float BETA = 0.25f;
    private static final int K = 4;
    private static final int WINDOW_MAX_SIZE = 500;
    private static final int WINDOW_INIT_SIZE = 1;
    private static final long NO_PENDING_SLEEP = 10;
    private static final long CA_THRESHOLD = 250;

    private UDPServer udpServer;
    private BestEffortBroadcast beb;

    private long nextSeqNum;
    public int thisHostId;
    private HashMap<Integer, HashMap<Long, PLMessage>> delivered;
    private Retransmit retransmitThread;
    private HashMap<Integer, Recipient> recipients;

    private static final int SEND = 7;
    private static final int GET_PENDING = 8;
    private static final int ACK_RECEIVED = 9;
    private static final int TO_EXPIRED = 10;


    public PerfectLinks(int thisHostId, int port, BestEffortBroadcast beb, Logger logger) {
        nextSeqNum = 0;
        this.thisHostId = thisHostId;
        delivered = new HashMap<>();
        recipients = new HashMap<>();

        this.beb = beb;
        this.udpServer = new UDPServer(port, this);
        udpServer.start();

        this.retransmitThread = new Retransmit();
        retransmitThread.start();
    }

    public void send(BEBMessage payload, int idRecipient) {
        PLMessage msg = new PLMessage(this.getNextSeqNum(), thisHostId, idRecipient, payload);
        accessRecipients(SEND, msg.getIdRecipient(), msg.getSeqNum(), msg);
    }

    public void sendAck(long seqNumToAck, int idRecipient, long recTime) {
        PLMessage msg = new PLMessage(ACK, thisHostId, idRecipient, seqNumToAck);
        udpServer.sendDatagram(PLMessage.getUdpPayloadFromPLMessage(msg), HostsParser.getHostById(msg.getIdRecipient()).getIpInet(), HostsParser.getHostById(msg.getIdRecipient()).getPort(), recTime);
    }

    public synchronized void udpDeliver(PLMessage msg, long received) {
        if (msg.getSeqNum() != ACK) {

            if (!delivered.containsKey(msg.getIdSender()) || !delivered.get(msg.getIdSender()).containsKey(msg.getSeqNum())) {
                if (delivered.containsKey(msg.getIdSender())) {
                    delivered.get(msg.getIdSender()).put(msg.getSeqNum(), msg);
                } else {
                    HashMap<Long, PLMessage> temp = new HashMap<>();
                    temp.put(msg.getSeqNum(), msg);
                    delivered.put(msg.getIdSender(), temp);
                }

                this.deliver(msg);
            }

            //this.deliver(msg);

            this.sendAck(msg.getSeqNum(), msg.getIdSender(), received);
        } else {
            this.ackReceived(msg);
        }
    }

    private void deliver(PLMessage msg) {
        beb.plDeliver(msg.getPayload());
    }

    private class Retransmit extends Thread {
        public void run() {
            while (true) {
                long nextTimeout = Long.MAX_VALUE;
                ArrayList<PLMessageTransmit> pending = (ArrayList<PLMessageTransmit>)accessRecipients(GET_PENDING, -1, -1, null);

                if(pending.size() == 0) {
                    nextTimeout = NO_PENDING_SLEEP;
                }
                else {
                    for (PLMessageTransmit msgRet : pending) {

                        long lastRet = msgRet.accessLastRetransmit(PLMessageTransmit.GET, -1);

                        long now = System.currentTimeMillis();
                        if (now - lastRet > msgRet.getRecipientTimeout()) {
                            //System.out.println("("+thisHostId +")"+": retrasmitting to "+ msgRet.getMsg().getIdRecipient() + ", to is "+ msgRet.getRecipientTimeout() + "; time elapsed "+ (now- lastRet));
                            udpServer.sendDatagram(PLMessage.getUdpPayloadFromPLMessage(msgRet.getMsg()), HostsParser.getHostById(msgRet.getMsg().getIdRecipient()).getIpInet(), HostsParser.getHostById(msgRet.getMsg().getIdRecipient()).getPort(), -1);

                            long retTime = System.currentTimeMillis();
                            msgRet.accessLastRetransmit(PLMessageTransmit.SET, retTime);

                            nextTimeout = Math.min(nextTimeout, msgRet.getRecipientTimeout());
                        } else {
                            nextTimeout = Math.min(nextTimeout, msgRet.getRecipientTimeout() - (now - lastRet));
                        }
                    }
                }

                try {
                    Thread.sleep((long)(nextTimeout));
                } catch (InterruptedException e) {
                    System.err.println("Sleep interrupted: " + e.getMessage());
                }
            }
        }
    }

    private long getNextSeqNum() {
        long seqNum = nextSeqNum;
        nextSeqNum++;
        return seqNum;
    }

    private void ackReceived(PLMessage ackMsg) {
        accessRecipients(ACK_RECEIVED, ackMsg.getIdSender(), ackMsg.getSeqNumToAck(), null);
    }

    private synchronized Object accessRecipients(int op, int idRecipient, long seqNum, PLMessage msg) {
        switch(op) {
            case SEND:
                if(!recipients.containsKey(idRecipient)) {
                    recipients.put(idRecipient, new Recipient());
                }

                Recipient rec = recipients.get(idRecipient);
                int maxWindowSize = rec.accessMaxWindowSize(Recipient.GET, -1);
                if(maxWindowSize - rec.window.size() > 0) {
                    rec.window.put(seqNum, new PLMessageTransmit(msg, System.currentTimeMillis()));
                    udpServer.sendDatagram(PLMessage.getUdpPayloadFromPLMessage(msg), HostsParser.getHostById(msg.getIdRecipient()).getIpInet(), HostsParser.getHostById(msg.getIdRecipient()).getPort(), -1);
                    //System.out.println("("+thisHostId +")"+" PUT IN WINDOW " +msg.getSeqNum() + ", window size " + rec.window.size() + "; queuing size " + rec.queuing.size());
                }
                else {
                    rec.queuing.add(msg);
                    //System.out.println("("+thisHostId +")"+" PUT IN QUEUE " +msg.getSeqNum() + ", window size " + rec.window.size() + "; queuing size " + rec.queuing.size());
                }
                return null;
            case GET_PENDING:
                ArrayList<PLMessageTransmit> pendingArray = new ArrayList<>();
                for(Integer id : recipients.keySet()) {
                    long timeout = recipients.get(id).getTimeout();
                    for(PLMessageTransmit msgPending : recipients.get(id).window.values()) {
                        msgPending.setRecipientTimeout(timeout);
                        pendingArray.add(msgPending);
                    }
                }
                return pendingArray;
            case ACK_RECEIVED:
                if(recipients.containsKey(idRecipient)) {
                    Recipient temp = recipients.get(idRecipient);
                    if(temp.window.containsKey(seqNum)) {
                        PLMessageTransmit msgInfo = recipients.get(idRecipient).window.remove(seqNum);

                        long lastRet = msgInfo.accessLastRetransmit(PLMessageTransmit.GET, -1);
                        long firstRet = msgInfo.getFirstTransmit();

                        if(lastRet == firstRet) {
                            long sampleRtt = System.currentTimeMillis() - lastRet;
                            if (temp.getRtt() == -1 || temp.getRttVar() == -1) {
                                temp.setRtt(sampleRtt);
                                temp.setRttVar(sampleRtt / 2);
                            } else {
                                temp.setRttVar((long) ((1 - BETA) * temp.getRttVar() + BETA * (Math.abs(temp.getRtt() - sampleRtt))));
                                temp.setRtt((long) ((1 - ALPHA) * temp.getRtt() + ALPHA * sampleRtt));
                            }
                            temp.setTimeout(temp.getRtt() + K * temp.getRttVar());

                            temp.accessMaxWindowSize(Recipient.UPDATE, 1);
                        }

                        temp.moveFromQueueToWindow();
                    }
                    else {
                        boolean found = false;
                        for(PLMessage queuingMsg : temp.queuing) {
                            if(queuingMsg.getSeqNum() == seqNum) {
                                found = true;
                                break;
                            }
                        }

                        if(found) {
                            temp.queuing.removeIf(plMessage -> plMessage.getSeqNum() == seqNum);
                        }
                    }
                }
                return null;
            case TO_EXPIRED:
                if(recipients.containsKey(idRecipient)) {
                    Recipient temp = recipients.get(idRecipient);
                    temp.accessMaxWindowSize(Recipient.UPDATE, -1);
                }
                return null;
            default:
                return null;
        }
    }

    private class PLMessageTransmit {
        private final PLMessage msg;
        private long lastRetransmit;
        private final long firstTransmit;
        private long recipientTimeout;

        public static final int GET = 2;
        public static final int SET = 3;

        public PLMessageTransmit(PLMessage msg, long transmitTime) {
            this.msg = msg;
            this.lastRetransmit = transmitTime;
            this.firstTransmit = transmitTime;
        }

        public synchronized long accessLastRetransmit(int op, long newRetTime) {
            switch (op) {
                case GET:
                    return lastRetransmit;
                case SET:
                    lastRetransmit = newRetTime;
                    return -1;
                default:
                    return -1;
            }
        }

        public PLMessage getMsg() {
            return msg;
        }

        public long getFirstTransmit() {
            return firstTransmit;
        }

        public long getRecipientTimeout() {
            return recipientTimeout;
        }

        public void setRecipientTimeout(long recipientTimeout) {
            this.recipientTimeout = recipientTimeout;
        }
    }

    private class Recipient {
        private long timeout;
        private long rtt;
        private long rttVar;
        private HashMap<Long, PLMessageTransmit> window;
        private Deque<PLMessage> queuing;
        private int notRetransmitted;
        private int maxWindowSize;

        public static final int GET = 0;
        public static final int UPDATE = 1;

        public Recipient() {
            timeout = INIT_TO;
            rtt = -1;
            rttVar = -1;

            window = new HashMap<>();
            queuing = new LinkedList<>();
            notRetransmitted = 0;
            maxWindowSize = WINDOW_INIT_SIZE;
        }

        public synchronized int accessMaxWindowSize(int op, int dir) {
            switch (op) {
                case GET:
                    return maxWindowSize;
                case UPDATE:
                    if(dir > 0) {
                        if(notRetransmitted >= 0) {
                            notRetransmitted++;
                        }
                        else {
                            notRetransmitted = 1;
                        }

                        if(maxWindowSize < CA_THRESHOLD) {
                            if(notRetransmitted == 2) {
                                maxWindowSize++;
                                notRetransmitted = 0;

                                moveFromQueueToWindow();
                            }
                        }
                        else if(maxWindowSize < WINDOW_MAX_SIZE) {
                            if (notRetransmitted == maxWindowSize) {
                                maxWindowSize++;
                                notRetransmitted = 0;

                                moveFromQueueToWindow();
                            }
                        }
                    }
                    else {
                        if(notRetransmitted < 0) {
                            notRetransmitted--;
                            if(Math.abs(notRetransmitted) - 1 >= maxWindowSize) {
                                resizeWindow();
                            }
                        }
                        else {
                            notRetransmitted = -1;
                            resizeWindow();
                        }
                    }
                    return -1;
                default:
                    return -1;
            }
        }

        private void resizeWindow() {
            int prevMaxSize = maxWindowSize;
            maxWindowSize = (int) Math.ceil(((double) prevMaxSize) / 1.4);

            ArrayList<PLMessageTransmit> toRemove = new ArrayList<>();

            Iterator it = window.values().iterator();
            for(int i = 0; i < (prevMaxSize - maxWindowSize) && it.hasNext(); i++) {
                toRemove.add((PLMessageTransmit) it.next());
            }

            for(PLMessageTransmit msgToRemove: toRemove) {
                window.remove(msgToRemove.getMsg().getSeqNum());
                queuing.addFirst(msgToRemove.getMsg());
            }
        }

        public long getTimeout() {
            return timeout;
        }

        public long getRtt() {
            return rtt;
        }

        public long getRttVar() {
            return rttVar;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }

        public void setRtt(long rtt) {
            this.rtt = rtt;
        }

        public void setRttVar(long rttVar) {
            this.rttVar = rttVar;
        }

        public void moveFromQueueToWindow() {
            while(window.size() < maxWindowSize && queuing.size() > 0) {
                PLMessage next = queuing.remove();

                udpServer.sendDatagram(PLMessage.getUdpPayloadFromPLMessage(next), HostsParser.getHostById(next.getIdRecipient()).getIpInet(), HostsParser.getHostById(next.getIdRecipient()).getPort(), -1);
                window.put(next.getSeqNum(), new PLMessageTransmit(next, System.currentTimeMillis()));

                //System.out.println("("+thisHostId +")"+" received ack MOVING FROM QUEUE TO WINDOW " +next.getSeqNum() + ", window size " + temp.window.size() + "; queuing size " + temp.queuing.size());
            }
        }
    }
}