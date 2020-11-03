package cs451.communication;

import java.io.Serializable;

public class FIFOMessage implements Serializable {
    private int seqNum;
    private int idBroadcaster;

    public FIFOMessage(int seqNum, int idBroadcaster) {
        this.seqNum = seqNum;
        this.idBroadcaster = idBroadcaster;
    }

    public int getSeqNum() {
        return seqNum;
    }

    public int getIdBroadcaster() {
        return idBroadcaster;
    }
}
