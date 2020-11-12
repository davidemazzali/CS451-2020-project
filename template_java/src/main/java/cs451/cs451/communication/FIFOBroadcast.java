package cs451.communication;

import cs451.utils.Host;
import cs451.utils.Logger;

import java.util.ArrayList;
import java.util.HashMap;

public class FIFOBroadcast {
    private UniformReliableBroadcast urb;

    private long nextSeqNum;
    private int thisHostId;
    private HashMap<Integer, HashMap<Long, FIFOMessage>> pending;
    private long [] next;

    private Logger logger;

    public FIFOBroadcast(int thisHostId, int port, ArrayList<Host> hosts, Logger logger) {
        nextSeqNum = 1;
        this.thisHostId = thisHostId;

        pending = new HashMap<>();
        next = new long[hosts.size()];
        for(int id = 1; id <= hosts.size(); id++) {
            next[id - 1] = 1;
        }

        this.logger = logger;

        urb = new UniformReliableBroadcast(thisHostId, port, hosts, this, logger);
    }

    public void broadcast() {
        FIFOMessage msg = new FIFOMessage(this.getNextSeqNum(), thisHostId);
        //logger.logBroadcast(msg.getSeqNum());
        urb.broadcast(msg);
    }

    public synchronized void urbDeliver(FIFOMessage msg) {
        if(!pending.containsKey(msg.getIdBroadcaster())) {
            pending.put(msg.getIdBroadcaster(), new HashMap<>());
        }

        if(msg.getSeqNum() >= next[msg.getIdBroadcaster()-1]) {
            pending.get(msg.getIdBroadcaster()).put(msg.getSeqNum(), msg);

            FIFOMessage msgToDeliver = this.canDeliver(msg.getIdBroadcaster());
            while (msgToDeliver != null) {
                next[msg.getIdBroadcaster() - 1]++;
                pending.get(msg.getIdBroadcaster()).remove(msgToDeliver.getSeqNum());
                this.deliver(msgToDeliver);

                msgToDeliver = this.canDeliver(msg.getIdBroadcaster());
            }
        }
    }

    private void deliver(FIFOMessage msg) {
        logger.logDeliver(msg.getIdBroadcaster(), msg.getSeqNum());
    }

    private FIFOMessage canDeliver(int idBroadcaster) {
        for(long seqNum : pending.get(idBroadcaster).keySet()) {
            if(seqNum == next[idBroadcaster-1]) {
                return pending.get(idBroadcaster).get(seqNum);
            }
        }

        return null;
    }

    private long getNextSeqNum() {
        long seqNum = nextSeqNum;
        nextSeqNum++;
        return seqNum;
    }
}