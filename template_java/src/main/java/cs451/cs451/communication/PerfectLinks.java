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
    private HashMap<Integer, Recipient> recipients; // for each recipient store timeout, SRTT, RTTVAR, pending messages (congestion window), queuing messages (waiting to be put in window)

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

        this.retransmitThread = new Retransmit();
        retransmitThread.start();
    }

    public void send(BEBMessage payload, int idRecipient) {
        PLMessage msg = new PLMessage(this.getNextSeqNum(), thisHostId, idRecipient, payload);
        accessRecipients(SEND, msg.getIdRecipient(), msg.getSeqNum(), msg);
    }

    public void sendAck(long seqNumToAck, int idRecipient, long recTime) {
        // instantiate ACK message and send it with UDP
        PLMessage msg = new PLMessage(ACK, thisHostId, idRecipient, seqNumToAck);
        udpServer.sendDatagram(PLMessage.getUdpPayloadFromPLMessage(msg), HostsParser.getHostById(msg.getIdRecipient()).getIpInet(), HostsParser.getHostById(msg.getIdRecipient()).getPort(), recTime);
    }

    public synchronized void udpDeliver(PLMessage msg, long received) {
        if (msg.getSeqNum() != ACK) { // if this is not an ACK

            // if not already delivered, deliver it and mark it as delivered
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

            // send ACK to sender
            this.sendAck(msg.getSeqNum(), msg.getIdSender(), received);
        } else {
            // handle ACK reception
            this.ackReceived(msg);
        }
    }

    private void deliver(PLMessage msg) {
        //System.out.println("("+thisHostId+") PL delivering");
        beb.plDeliver(msg.getPayload());
    }

    private class Retransmit extends Thread {
        public void run() {
            String alive;
            while (true) {/*
                if(udpServer.isInterrupted()) {
                    alive = "IS alive";
                }
                else {
                    alive = "IS NOT alive";
                }
                System.out.println("("+thisHostId+") udp server "  + alive );*/
                long nextTimeout = Long.MAX_VALUE; // stores how soon will a timeout expire, determines how long this thread sleeps at the end of loop

                // get messages waiting to be ACKed
                ArrayList<PLMessageTransmit> pending = (ArrayList<PLMessageTransmit>)accessRecipients(GET_PENDING, -1, -1, null);
                //printPending(pending);
                if(pending.size() == 0) {
                    nextTimeout = NO_PENDING_SLEEP;
                }
                else {
                    for (PLMessageTransmit msgRet : pending) {

                        // last time when this messages was (re)sent
                        long lastRet = msgRet.accessLastRetransmit(PLMessageTransmit.GET, -1);

                        long now = System.currentTimeMillis();
                        if (now - lastRet > msgRet.getRecipientTimeout()) { // if the timeout associated to this message' sender is expired
                            udpServer.sendDatagram(PLMessage.getUdpPayloadFromPLMessage(msgRet.getMsg()), HostsParser.getHostById(msgRet.getMsg().getIdRecipient()).getIpInet(), HostsParser.getHostById(msgRet.getMsg().getIdRecipient()).getPort(), -1);
                            //System.out.println("("+thisHostId+") re-transmitting to "+HostsParser.getHostById(msgRet.getMsg().getIdRecipient()).getIpInet() +" "+ HostsParser.getHostById(msgRet.getMsg().getIdRecipient()).getPort());
                            // update the retransmission time
                            long retTime = System.currentTimeMillis();
                            msgRet.accessLastRetransmit(PLMessageTransmit.SET, retTime);

                            nextTimeout = Math.min(nextTimeout, msgRet.getRecipientTimeout());

                            //accessRecipients(TO_EXPIRED, msgRet.getMsg().getIdRecipient(),  -1, null);
                        } else {
                            nextTimeout = Math.min(nextTimeout, msgRet.getRecipientTimeout() - (now - lastRet));
                        }
                    }
                }

                try {
                    Thread.sleep((long)(1000));
                } catch (InterruptedException e) {
                    System.err.println("Sleep interrupted: " + e.getMessage());
                }
            }
        }
    }

    // get next sequence number and increment it
    private long getNextSeqNum() {
        long seqNum = nextSeqNum;
        nextSeqNum++;
        return seqNum;
    }

    private void ackReceived(PLMessage ackMsg) {
        accessRecipients(ACK_RECEIVED, ackMsg.getIdSender(), ackMsg.getSeqNumToAck(), null);
    }

    // thread-safe method to access info on each recipient
    private synchronized Object accessRecipients(int op, int idRecipient, long seqNum, PLMessage msg) {
        switch(op) {
            case SEND:
                // if there is room for the new message in the recipient's associated congestion window, send it
                // otherwise let the message queue

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
                // get all messages (destined to all recipients) in congestion windows (i.e. those waiting for an ACK)

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
                    if(temp.window.containsKey(seqNum)) { // if there is congestion window containing the message with the ACKed sequence number
                        PLMessageTransmit msgInfo = recipients.get(idRecipient).window.remove(seqNum);

                        long lastRet = msgInfo.accessLastRetransmit(PLMessageTransmit.GET, -1);
                        long firstRet = msgInfo.getFirstTransmit();

                        if(lastRet == firstRet) { // if the corresponding message was sent only once (no retransmit)
                            // update RTT, RTTVAR and timeout for this recipient
                            long sampleRtt = System.currentTimeMillis() - lastRet;
                            if (temp.getRtt() == -1 || temp.getRttVar() == -1) {
                                temp.setRtt(sampleRtt);
                                temp.setRttVar(sampleRtt / 2);
                            } else {
                                temp.setRttVar((long) ((1 - BETA) * temp.getRttVar() + BETA * (Math.abs(temp.getRtt() - sampleRtt))));
                                temp.setRtt((long) ((1 - ALPHA) * temp.getRtt() + ALPHA * sampleRtt));
                            }
                            temp.setTimeout(temp.getRtt() + K * temp.getRttVar());

                            // inform congestion windows size manager method that a message was ACKed without being retransmitted
                            temp.accessMaxWindowSize(Recipient.UPDATE, 1);
                        }

                        // move all messages in queue that now fit into the window
                        temp.moveFromQueueToWindow();
                    }
                    else {
                        // if the ACKed message is in a queue, this means that the message was in the congestion window but has been later removed and re-put into queue
                        // (this happens when the congestion window is made smaller)

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

    private void printPending(ArrayList<PLMessageTransmit> old) {
        HashMap<Integer, Long> num = new HashMap<>();
        for(PLMessageTransmit msg : old) {
            if(!num.containsKey(msg.getMsg().getIdRecipient())) {
                num.put(msg.getMsg().getIdRecipient(), 0L);
            }
            num.put(msg.getMsg().getIdRecipient(), num.get(msg.getMsg().getIdRecipient())+1);
        }

        System.out.print("("+thisHostId+")  ");
        for(Map.Entry<Integer, Long> e: num.entrySet()) {
            System.out.print("p:"+e.getKey() + "-"+e.getValue()+"  ");
        }
        System.out.println();
    }

    private class PLMessageTransmit { // store info about sent message waiting to be acked
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

        // thread-safe method to access lastRetransmit field
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
        // for each recipient store timeout, SRTT, RTTVAR, pending messages (congestion window), queuing messages (waiting to be put in window)
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

        // resize (make bigger or smaller) the window, depending on whether messages are being ACKed on time or not
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
                            if(Math.abs(notRetransmitted) >= maxWindowSize) {
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

        // make window size smaller and re-put extra messages into queue
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

        // move all messages in the queue that fit into the congestion window, in the window
        public void moveFromQueueToWindow() {
            while(window.size() < maxWindowSize && queuing.size() > 0) {
                PLMessage next = queuing.remove();

                udpServer.sendDatagram(PLMessage.getUdpPayloadFromPLMessage(next), HostsParser.getHostById(next.getIdRecipient()).getIpInet(), HostsParser.getHostById(next.getIdRecipient()).getPort(), -1);
                window.put(next.getSeqNum(), new PLMessageTransmit(next, System.currentTimeMillis()));
            }
        }
    }
}
/*
public class PerfectLinks {
    private static final long ACK = -1;
    private static final long INIT_TO = 1000;
    private static final float ALPHA = 0.125f;
    private static final float BETA = 0.25f;
    private static final int K = 4;
    private static final int WINDOW_MAX_SIZE = 1000;
    private static final int WINDOW_INIT_SIZE = 1;
    private static final long NO_PENDING_SLEEP = 10;
    private static final long CA_THRESHOLD = 250;

    private UDPServer udpServer;
    private BestEffortBroadcast beb;

    private long nextSeqNum;
    public int thisHostId;
    private HashMap<Integer, HashMap<Long, PLMessage>> delivered;
    private Retransmit retransmitThread;
    private HashMap<Integer, Recipient> recipients; // for each recipient store timeout, SRTT, RTTVAR, pending messages (congestion window), queuing messages (waiting to be put in window)

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
        // instantiate ACK message and send it with UDP
        PLMessage msg = new PLMessage(ACK, thisHostId, idRecipient, seqNumToAck);
        udpServer.sendDatagram(PLMessage.getUdpPayloadFromPLMessage(msg), HostsParser.getHostById(msg.getIdRecipient()).getIpInet(), HostsParser.getHostById(msg.getIdRecipient()).getPort(), recTime);
    }

    public synchronized void udpDeliver(PLMessage msg, long received) {
        if (msg.getSeqNum() != ACK) { // if this is not an ACK

            // if not already delivered, deliver it and mark it as delivered
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

            // send ACK to sender
            this.sendAck(msg.getSeqNum(), msg.getIdSender(), received);
        } else {
            // handle ACK reception
            this.ackReceived(msg);
        }
    }

    private void deliver(PLMessage msg) {
        beb.plDeliver(msg.getPayload());
    }

    private class Retransmit extends Thread {
        public void run() {
            while (true) {
                long nextTimeout = Long.MAX_VALUE; // stores how soon will a timeout expire, determines how long this thread sleeps at the end of loop

                // get messages waiting to be ACKed
                ArrayList<PLMessageTransmit> pending = (ArrayList<PLMessageTransmit>)accessRecipients(GET_PENDING, -1, -1, null);

                if(pending.size() == 0) {
                    nextTimeout = NO_PENDING_SLEEP;
                }
                else {
                    for (PLMessageTransmit msgRet : pending) {

                        // last time when this messages was (re)sent
                        long lastRet = msgRet.accessLastRetransmit(PLMessageTransmit.GET, -1);

                        long now = System.currentTimeMillis();
                        if (now - lastRet > msgRet.getRecipientTimeout()) { // if the timeout associated to this message' sender is expired
                            udpServer.sendDatagram(PLMessage.getUdpPayloadFromPLMessage(msgRet.getMsg()), HostsParser.getHostById(msgRet.getMsg().getIdRecipient()).getIpInet(), HostsParser.getHostById(msgRet.getMsg().getIdRecipient()).getPort(), -1);

                            // update the retransmission time
                            long retTime = System.currentTimeMillis();
                            msgRet.accessLastRetransmit(PLMessageTransmit.SET, retTime);

                            nextTimeout = Math.min(nextTimeout, msgRet.getRecipientTimeout());
                            accessRecipients(TO_EXPIRED, msgRet.getMsg().getIdRecipient(),  -1, null);
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

    // get next sequence number and increment it
    private long getNextSeqNum() {
        long seqNum = nextSeqNum;
        nextSeqNum++;
        return seqNum;
    }

    private void ackReceived(PLMessage ackMsg) {
        accessRecipients(ACK_RECEIVED, ackMsg.getIdSender(), ackMsg.getSeqNumToAck(), null);
    }

    // thread-safe method to access info on each recipient
    private synchronized Object accessRecipients(int op, int idRecipient, long seqNum, PLMessage msg) {
        switch(op) {
            case SEND:
                // if there is room for the new message in the recipient's associated congestion window, send it
                // otherwise let the message queue

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
                // get all messages (destined to all recipients) in congestion windows (i.e. those waiting for an ACK)

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
                    if(temp.window.containsKey(seqNum)) { // if there is congestion window containing the message with the ACKed sequence number
                        PLMessageTransmit msgInfo = recipients.get(idRecipient).window.remove(seqNum);

                        long lastRet = msgInfo.accessLastRetransmit(PLMessageTransmit.GET, -1);
                        long firstRet = msgInfo.getFirstTransmit();

                        if(lastRet == firstRet) { // if the corresponding message was sent only once (no retransmit)
                            // update RTT, RTTVAR and timeout for this recipient
                            long sampleRtt = System.currentTimeMillis() - lastRet;
                            if (temp.getRtt() == -1 || temp.getRttVar() == -1) {
                                temp.setRtt(sampleRtt);
                                temp.setRttVar(sampleRtt / 2);
                            } else {
                                temp.setRttVar((long) ((1.0 - BETA) * (double)temp.getRttVar() + BETA * (double)(Math.abs(temp.getRtt() - sampleRtt))));
                                temp.setRtt((long) ((1.0 - ALPHA) * (double)temp.getRtt() + ALPHA * (double)sampleRtt));
                            }
                            temp.setTimeout(temp.getRtt() + K * temp.getRttVar());

                            // inform congestion windows size manager method that a message was ACKed without being retransmitted
                            temp.accessMaxWindowSize(Recipient.UPDATE, 1);
                        }

                        // move all messages in queue that now fit into the window
                        temp.moveFromQueueToWindow();
                    }
                    else {
                        // if the ACKed message is in a queue, this means that the message was in the congestion window but has been later removed and re-put into queue
                        // (this happens when the congestion window is made smaller)

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

    private class PLMessageTransmit { // store info about sent message waiting to be acked
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

        // thread-safe method to access lastRetransmit field
        public synchronized long accessLastRetransmit(int op, long newRetTime) {
            switch (op) {
                case PLMessageTransmit.GET:
                    return lastRetransmit;
                case PLMessageTransmit.SET:
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
        // for each recipient store timeout, SRTT, RTTVAR, pending messages (congestion window), queuing messages (waiting to be put in window)
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

        // resize (make bigger or smaller) the window, depending on whether messages are being ACKed on time or not
        public synchronized int accessMaxWindowSize(int op, int dir) {
            switch (op) {
                case Recipient.GET:
                    return maxWindowSize;
                case Recipient.UPDATE:
                    if(dir > 0) {
                        if(notRetransmitted >= 0) {
                            notRetransmitted++;
                        }
                        else {
                            notRetransmitted = 1;
                        }

                        if(maxWindowSize < CA_THRESHOLD) {
                            if(notRetransmitted == 1) {
                                maxWindowSize++;
                                notRetransmitted = 0;

                                //System.out.println("----- (" + thisHostId + ")" + maxWindowSize);

                                moveFromQueueToWindow();
                            }
                        }
                        else if(maxWindowSize < WINDOW_MAX_SIZE) {
                            if (notRetransmitted == maxWindowSize) {
                                maxWindowSize++;
                                notRetransmitted = 0;

                                //System.out.println("@@@@@ (" + thisHostId + ") " + maxWindowSize);

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
                            //resizeWindow();
                        }
                    }
                    return -1;
                default:
                    return -1;
            }
        }

        // make window size smaller and re-put extra messages into queue
        private void resizeWindow() {
            int prevMaxSize = maxWindowSize;
            maxWindowSize = (int) Math.ceil(((double) prevMaxSize) / 1.4);

            //timeout *= 2;

            //System.out.println("***** (" + thisHostId + ")" + maxWindowSize);

            /*
            ArrayList<PLMessageTransmit> toRemove = new ArrayList<>();

            Iterator it = window.values().iterator();
            for(int i = 0; i < (prevMaxSize - maxWindowSize) && it.hasNext(); i++) {
                toRemove.add((PLMessageTransmit) it.next());
            }

            for(PLMessageTransmit msgToRemove: toRemove) {
                window.remove(msgToRemove.getMsg().getSeqNum());
                queuing.addFirst(msgToRemove.getMsg());
            }

             */
/*
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

        // move all messages in the queue that fit into the congestion window, in the window
        public void moveFromQueueToWindow() {
            while(window.size() < maxWindowSize && queuing.size() > 0) {
                PLMessage next = queuing.remove();

                udpServer.sendDatagram(PLMessage.getUdpPayloadFromPLMessage(next), HostsParser.getHostById(next.getIdRecipient()).getIpInet(), HostsParser.getHostById(next.getIdRecipient()).getPort(), -1);
                window.put(next.getSeqNum(), new PLMessageTransmit(next, System.currentTimeMillis()));
            }
        }
    }
}
*/