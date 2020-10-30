package cs451.communication;

import java.io.Serializable;

public class URBMessage implements Serializable {
    private int seqNum;
    private int idBroadcaster;

    public URBMessage(int seqNum, int idBroadcaster) {
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
