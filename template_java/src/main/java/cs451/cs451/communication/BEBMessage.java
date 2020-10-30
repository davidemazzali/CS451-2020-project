package cs451.communication;

import java.io.*;

public class BEBMessage implements Serializable{
    private int seqNum;
    private int idSender;
    private URBMessage payload;

    public BEBMessage(int seqNum, int idSender, URBMessage payload) {
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

    public URBMessage getPayload() {
        return payload;
    }
}
