package cs451.communication;

import cs451.utils.Host;
import cs451.utils.Logger;

import java.util.ArrayList;
import java.util.HashMap;

public class FIFOBroadcast implements TopLevelBroadcast{
    private UniformReliableBroadcast urb;

    private long nextSeqNum;
    private int thisHostId;

    // first key is host id, second key is the seq. number of the message
    private HashMap<Integer, HashMap<Long, FIFOMessage>> pending; // store the messages this process is waiting to deliver that were originally broadcast by each member of the network

    // next seq. number to deliver for each network member (index is ID - 1)
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
        urb.broadcast(msg);
    }

    public synchronized void urbDeliver(TopLevelMessage tlMsg) {
        FIFOMessage msg = (FIFOMessage)tlMsg;
        if(!pending.containsKey(msg.getIdBroadcaster())) {
            pending.put(msg.getIdBroadcaster(), new HashMap<>());
        }

        if(msg.getSeqNum() >= next[msg.getIdBroadcaster()-1]) {
            // put this message into pending
            pending.get(msg.getIdBroadcaster()).put(msg.getSeqNum(), msg);

            // as long as there are messages that can be delivered, deliver them and remove them from pending
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

    // return a message that can be delivered if any, null otherwise
    private FIFOMessage canDeliver(int idBroadcaster) {
        for(long seqNum : pending.get(idBroadcaster).keySet()) {
            if(seqNum == next[idBroadcaster-1]) {
                return pending.get(idBroadcaster).get(seqNum);
            }
        }

        return null;
    }

    // get next sequence number and increment
    private long getNextSeqNum() {
        long seqNum = nextSeqNum;
        nextSeqNum++;
        return seqNum;
    }
}