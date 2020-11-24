package cs451.communication;

import java.io.*;

public class BEBMessage implements Serializable{
    private long seqNum;
    private int idSender;
    private URBMessage payload;

    public BEBMessage(long seqNum, int idSender, URBMessage payload) {
        this.seqNum = seqNum;
        this.idSender = idSender;
        this.payload = payload;
    }

    public long getSeqNum() {
        return seqNum;
    }

    public int getIdSender() {
        return idSender;
    }

    public URBMessage getPayload() {
        return payload;
    }
}
