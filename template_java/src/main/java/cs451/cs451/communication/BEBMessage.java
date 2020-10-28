package cs451.communication;

import cs451.parser.HostsParser;

import java.io.*;
import java.net.InetAddress;

public class BEBMessage implements Serializable{
    private int seqNum;
    private int idSender;
    private byte[] payload;

    public BEBMessage(int seqNum, int idSender, byte[] payload) {
        this.seqNum = seqNum;
        this.idSender = idSender;
        this.payload = payload;
    }

    public int getSeqNum() {
        return seqNum;
    }

    public int getIdSender() {
        return idSender;
    }

    public byte[] getPayload() {
        return payload;
    }

    public static byte [] getPLPayloadFromBEBMessage(BEBMessage msg) {
        byte [] bytesPacket = null;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(out);
            os.writeObject(msg);
            bytesPacket = out.toByteArray();
        } catch (IOException e) {
            System.err.println("Error serializing BEB message: " + e.getMessage());
        }

        return bytesPacket;
    }

    public static synchronized BEBMessage getBEBMessageFromPLPayload(byte [] plPayload) {
        BEBMessage msg = null;
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(plPayload);
            ObjectInputStream is = new ObjectInputStream(in);
            msg = (BEBMessage) is.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error deserializing BEB packet: " + e.getMessage());
        }
        return msg;
    }
}
