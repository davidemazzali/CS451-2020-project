package cs451.communication;

import cs451.parser.HostsParser;
import cs451.utils.Constants;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.sql.Time;
import java.util.HashMap;

public class PerfectLinks {
    private static final int ACK = -1;

    private int thisHostId;
    private int nextMsgId;
    private HashMap<Integer, PLMessage> delivered;
    private HashMap<Integer, PLMessage> pending;
    private DatagramSocket socket;

    public PerfectLinks(int thisHostId, DatagramSocket socket) {
        this.thisHostId = thisHostId;
        this.socket = socket;

        delivered = new HashMap<>();
        pending = new HashMap<>();
        nextMsgId = 0;
    }

    public void send(int seqNum, byte [] payload, int idRecipient) {
        PLMessage msg = new PLMessage(getNextMsgId(), seqNum, thisHostId, idRecipient, payload);
        UDPSendService.sendDatagram(socket, msg.getPLPacket(), HostsParser.getHostById(msg.getIdRecipient()).getIpInet(), HostsParser.getHostById(msg.getIdRecipient()).getPort());
        pending.put(msg.getMsgId(), msg);
        Timeout to = new Timeout(msg);
        to.start();
    }

    public void sendAck(int seqNumToAck, int idRecipient) {
        byte [] payload = ByteBuffer.allocate(Constants.INT_BYTES_SIZE).putInt(seqNumToAck).array();
        PLMessage msg = new PLMessage(getNextMsgId(), ACK, thisHostId, idRecipient, payload);
        UDPSendService.sendDatagram(socket, msg.getPLPacket(), HostsParser.getHostById(msg.getIdRecipient()).getIpInet(), HostsParser.getHostById(msg.getIdRecipient()).getPort());
    }

    public void deliver() {

    }

    private class Timeout extends Thread {
        private static final int INITIAL_TO = 100;
        private static final float INCREASE_FACTOR = 1.5f;
        private int timeout;
        private PLMessage msg;

        public Timeout(PLMessage msgId) {
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

                if(pending.containsKey(msg.getMsgId())) {
                    UDPSendService.sendDatagram(socket, msg.getPLPacket(), HostsParser.getHostById(msg.getIdRecipient()).getIpInet(), HostsParser.getHostById(msg.getIdRecipient()).getPort());
                }

                timeout *= INCREASE_FACTOR;

            } while(pending.containsKey(msg.getMsgId()));
        }
    }

    private synchronized int getNextMsgId() {
        int msgId = nextMsgId;
        nextMsgId++;
        return msgId;
    }
}
