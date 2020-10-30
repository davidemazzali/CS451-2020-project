package cs451.communication;

import cs451.parser.HostsParser;

import java.io.*;
import java.net.InetAddress;

public class PLMessage implements Serializable{
    private int seqNum;
    private int idSender;
    private int idRecipient;
    private BEBMessage payload;
    private int seqNumToAck;

    public PLMessage(int seqNum, int idSender, int idRecipient, BEBMessage payload) {
        this.seqNum = seqNum;
        this.idSender = idSender;
        this.idRecipient = idRecipient;
        this.payload = payload;
        this.seqNumToAck = -1;
    }

    public PLMessage(int seqNum, int idSender, int idRecipient, int seqNumToAck) {
        this.seqNum = seqNum;
        this.idSender = idSender;
        this.idRecipient = idRecipient;
        this.payload = null;
        this.seqNumToAck = seqNumToAck;
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

    public BEBMessage getPayload() {
        return payload;
    }

    public int getSeqNumToAck() {
        return seqNumToAck;
    }

    public static byte [] getUdpPayloadFromPLMessage(PLMessage msg) {
        byte [] bytesPacket = null;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(out);
            os.writeObject(msg);
            bytesPacket = out.toByteArray();
        } catch (IOException e) {
            System.err.println("Error serializing PL packet: " + e.getMessage());
        }

        return bytesPacket;
    }

    public static synchronized PLMessage getPLMessageFromUdpPayload(byte [] udpPayload) {
        PLMessage msg = null;
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(udpPayload);
            ObjectInputStream is = new ObjectInputStream(in);
            msg = (PLMessage) is.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.err.println("Error deserializing PL packet: " + e.getMessage());
        }
        return msg;
    }
}
