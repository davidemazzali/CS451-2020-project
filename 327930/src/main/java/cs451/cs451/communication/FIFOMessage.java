package cs451.communication;

import java.io.Serializable;

public class FIFOMessage implements Serializable {
    private long seqNum;
    private int idBroadcaster;

    public FIFOMessage(long seqNum, int idBroadcaster) {
        this.seqNum = seqNum;
        this.idBroadcaster = idBroadcaster;
    }

    public long getSeqNum() {
        return seqNum;
    }

    public int getIdBroadcaster() {
        return idBroadcaster;
    }
}
