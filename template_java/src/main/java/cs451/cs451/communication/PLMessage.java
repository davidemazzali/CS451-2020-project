package cs451.communication;

import cs451.parser.HostsParser;

import java.io.*;
import java.net.InetAddress;

public class PLMessage {
    private int seqNum;
    private int idSender;
    private int idRecipient;
    private byte[] payload;

    public PLMessage(int seqNum, int idSender, int idRecipient, byte[] payload) {
        this.seqNum = seqNum;
        this.idSender = idSender;
        this.idRecipient = idRecipient;
        this.payload = payload;
    }

    public int getSeqNum() {
        return seqNum;
    }

    public int getIdSender() {
        return idSender;
    }

    public int getIdRecipient() {
        return idRecipient;
    }

    public byte[] getPayload() {
        return payload;
    }

    public static byte [] getUdpPayloadFromPLMessage(PLMessage msg) {
        byte [] bytesPacket = null;
        try {
            PLPacket packet = new PLPacket(msg.getSeqNum(), msg.getPayload());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(out);
            os.writeObject(packet);
            bytesPacket = out.toByteArray();
        } catch (IOException e) {
            System.err.println("Error serializing PL packet: " + e.getMessage());
        }

        return bytesPacket;
    }

    public static synchronized PLMessage getPLMessageFromUdpPayload(int senderPort, InetAddress senderAddress, int thisHostId, byte [] udpPayload) {
        PLMessage msg = null;
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(udpPayload);
            ObjectInputStream is = new ObjectInputStream(in);
            PLPacket packet = (PLPacket) is.readObject();

            int senderId = HostsParser.getIdByPortAddr(senderPort, senderAddress);

            msg = new PLMessage(packet.getSeqNum(), senderId, thisHostId, packet.getPayload());
        } catch (IOException | ClassNotFoundException e) {
            //System.err.println("Error deserializing PL packet: " + e);
            e.printStackTrace();
        }
        return msg;
    }

    public static class PLPacket implements Serializable {
        private int seqNum;
        private byte[] payload;

        public PLPacket(int seqNum, byte[] payload) {
            this.seqNum = seqNum;
            this.payload = payload;
        }

        private int getSeqNum() {
            return seqNum;
        }

        private byte[] getPayload() {
            return payload;
        }
    }
}
