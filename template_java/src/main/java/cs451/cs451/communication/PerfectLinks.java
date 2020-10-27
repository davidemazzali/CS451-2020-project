package cs451.communication;

import cs451.parser.HostsParser;
import cs451.utils.Constants;
import cs451.utils.Logger;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.sql.Time;
import java.util.HashMap;

public class PerfectLinks {
    private static final int ACK = -1;

    private int nextSeqNum;

    private int thisHostId;
    private HashMap<Integer, HashMap<Integer, PLMessage>> delivered;
    private HashMap<Integer, PLMessage> pending;
    private UDPServer udpServer;
    private Logger logger;

    public PerfectLinks(int thisHostId, DatagramSocket socket, Logger logger) {
        nextSeqNum = 0;

        this.thisHostId = thisHostId;

        delivered = new HashMap<>();
        pending = new HashMap<>();

        this.logger = logger;

        this.udpServer = new UDPServer(thisHostId, socket, this);
        udpServer.start();
    }

    public void send(byte [] payload, int idRecipient) {
        PLMessage msg = new PLMessage(this.getNextSeqNum(), thisHostId, idRecipient, payload);
        udpServer.sendDatagram(PLMessage.getUdpPayloadFromPLMessage(msg), HostsParser.getHostById(msg.getIdRecipient()).getIpInet(), HostsParser.getHostById(msg.getIdRecipient()).getPort());

        logger.logSend(msg.getIdRecipient(), msg.getSeqNum());

        pending.put(msg.getSeqNum(), msg);
        Timeout to = new Timeout(msg);
        to.start();
    }

    public void sendAck(int seqNumToAck, int idRecipient) {
        byte [] payload = ByteBuffer.allocate(Constants.INT_BYTES_SIZE).putInt(seqNumToAck).array();
        PLMessage msg = new PLMessage(ACK, thisHostId, idRecipient, payload);
        udpServer.sendDatagram(PLMessage.getUdpPayloadFromPLMessage(msg), HostsParser.getHostById(msg.getIdRecipient()).getIpInet(), HostsParser.getHostById(msg.getIdRecipient()).getPort());

        //System.out.println("********* ack sent " + idRecipient + " " + seqNumToAck);
    }

    public synchronized void udpDeliver(PLMessage msg) {
        if(msg.getSeqNum() != ACK) {
            if (!delivered.containsKey(msg.getIdSender()) || !delivered.get(msg.getIdSender()).containsKey(msg.getSeqNum())) {
                if (delivered.containsKey(msg.getIdSender())) {
                    delivered.get(msg.getIdSender()).put(msg.getSeqNum(), msg);
                } else {
                    HashMap<Integer, PLMessage> temp = new HashMap<>();
                    temp.put(msg.getSeqNum(), msg);
                    delivered.put(msg.getIdSender(), temp);
                }

                this.deliver(msg);
            }

            this.sendAck(msg.getSeqNum(), msg.getIdSender());
        }
        else {
            this.ackReceived(msg);
        }
    }

    public void deliver(PLMessage msg) {
        logger.logDeliver(msg.getIdSender(), msg.getSeqNum());
    }

    private class Timeout extends Thread {
        private static final int INITIAL_TO = 200;
        private static final float INCREASE_FACTOR = 1.0f;
        private int timeout;
        private PLMessage msg;

        public Timeout(PLMessage msg) {
            this.msg = msg;
            timeout = INITIAL_TO;
        }

        public void run() {
            do {
                try {
                    sleep(timeout);
                } catch (InterruptedException e) {
                    System.err.println("Sleep interrupted: " + e.getMessage());
                }

                if(pending.containsKey(msg.getSeqNum())) {
                    udpServer.sendDatagram(PLMessage.getUdpPayloadFromPLMessage(msg), HostsParser.getHostById(msg.getIdRecipient()).getIpInet(), HostsParser.getHostById(msg.getIdRecipient()).getPort());
                    //System.out.println("############# retransmit " + msg.getIdRecipient() + " " + msg.getSeqNum());
                }

                timeout *= INCREASE_FACTOR;

            } while(pending.containsKey(msg.getSeqNum()));
        }
    }

    private int getNextSeqNum() {
        int seqNum = nextSeqNum;
        nextSeqNum++;
        return seqNum;
    }

    private synchronized void ackReceived(PLMessage ackMsg) {
        int ackedMsgSeqNum = ByteBuffer.wrap(ackMsg.getPayload()).getInt();
        if(pending.containsKey(ackedMsgSeqNum)) {
            pending.remove(ackedMsgSeqNum);
        }

        //System.out.println("********* ack received " + ackMsg.getIdSender() + " " + ackedMsgSeqNum);
        //System.out.println("~~~~~~~~~~~~~~~~~~~~~~~ " + pending.size());
    }
}
