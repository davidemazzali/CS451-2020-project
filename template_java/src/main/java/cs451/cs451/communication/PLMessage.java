package cs451.communication;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class PLMessage {
    private int msgId;
    private int seqNum;
    private int idSender;
    private int idRecipient;
    private byte[] payload;

    public PLMessage(int msgId, int seqNum, int idSender, int idRecipient, byte[] payload) {
        this.msgId = msgId;
        this.seqNum = seqNum;
        this.idSender = idSender;
        this.idRecipient = idRecipient;
        this.payload = payload;
    }

    public int getMsgId() {
        return msgId;
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

    public byte [] getPLPacket() {
        byte [] bytesPacket = null;
        try {
            PLPacket packet = new PLPacket(seqNum, payload);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(out);
            os.writeObject(packet);
            bytesPacket = out.toByteArray();
        } catch (IOException e) {
            System.err.println("Error serializing PL packet: " + e.getMessage());
        }

        return bytesPacket;
    }

    private class PLPacket {
        private int seqNum;
        private byte[] payload;

        public PLPacket(int seqNum, byte[] payload) {
            this.seqNum = seqNum;
            this.payload = payload;
        }
    }
}
