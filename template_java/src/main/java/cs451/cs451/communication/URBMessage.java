package cs451.communication;

import java.io.Serializable;

public class URBMessage implements Serializable {
    private long seqNum;
    private int idBroadcaster;
    private TopLevelMessage payload;

    public URBMessage(long seqNum, int idBroadcaster, TopLevelMessage payload) {
        this.seqNum = seqNum;
        this.idBroadcaster = idBroadcaster;
        this.payload = payload;
    }

    public long getSeqNum() {
        return seqNum;
    }

    public int getIdBroadcaster() {
        return idBroadcaster;
    }

    public TopLevelMessage getPayload() {
        return payload;
    }
}
