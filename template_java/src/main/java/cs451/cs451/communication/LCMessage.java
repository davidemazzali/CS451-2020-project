package cs451.communication;

import java.util.HashMap;

public class LCMessage implements TopLevelMessage {
    private long seqNum;
    private int idBroadcaster;
    private HashMap<Integer, Long> clocks;

    public LCMessage(long seqNum, int idBroadcaster, HashMap<Integer, Long> clocks) {
        this.seqNum = seqNum;
        this.idBroadcaster = idBroadcaster;
        this.clocks = clocks;
    }

    public long getSeqNum() {
        return seqNum;
    }

    public int getIdBroadcaster() {
        return idBroadcaster;
    }

    public HashMap<Integer, Long> getClocks() {
        return clocks;
    }
}
