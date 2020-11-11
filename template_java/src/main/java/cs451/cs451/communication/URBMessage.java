package cs451.communication;

import java.io.Serializable;

public class URBMessage implements Serializable {
    private int seqNum;
    private int idBroadcaster;
    private FIFOMessage payload;

    public URBMessage(int seqNum, int idBroadcaster, FIFOMessage payload) {
        this.seqNum = seqNum;
        this.idBroadcaster = idBroadcaster;
        this.payload = payload;
    }

    public int getSeqNum() {
        return seqNum;
    }

    public int getIdBroadcaster() {
        return idBroadcaster;
    }

    public FIFOMessage getPayload() {
        return payload;
    }
}