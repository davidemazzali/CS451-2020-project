package cs451.communication;

import cs451.utils.Host;
import cs451.utils.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LocalizedCausalBroadcast  implements TopLevelBroadcast{
    private UniformReliableBroadcast urb;

    private long nextSeqNum;
    private int thisHostId;

    // first key is host id, second key is the seq. number of the message
    private HashMap<Integer, HashMap<Long, LCMessage>> pending; // store the messages this process is waiting to deliver that were originally broadcast by each member of the network

    private ArrayList<Integer> depending;
    // next seq. number to deliver for each network member (index is ID - 1)
    private long [] next;

    private Logger logger;

    public LocalizedCausalBroadcast(int thisHostId, int port, ArrayList<Host> hosts, ArrayList<Integer> depending, Logger logger) {
        nextSeqNum = 1;
        this.thisHostId = thisHostId;

        pending = new HashMap<>();
        this.depending = depending;

        next = new long[hosts.size()];
        for(int id = 1; id <= hosts.size(); id++) {
            next[id - 1] = 0;
        }

        this.logger = logger;

        urb = new UniformReliableBroadcast(thisHostId, port, hosts, this, logger);
    }

    public void broadcast() {
        long seqNum = this.getNextSeqNum();
        HashMap<Integer, Long> copyClocks = new HashMap<>();
        for(int dependenceId : depending) {
            copyClocks.put(dependenceId, next[dependenceId-1]);
        }
        copyClocks.put(thisHostId, seqNum - 1);

        LCMessage msg = new LCMessage(seqNum, thisHostId, copyClocks);
        urb.broadcast(msg);
    }

    public synchronized void urbDeliver(TopLevelMessage tlMsg) {
        LCMessage msg = (LCMessage)tlMsg;
        if(!pending.containsKey(msg.getIdBroadcaster())) {
            pending.put(msg.getIdBroadcaster(), new HashMap<>());
        }

        if(msg.getSeqNum() >= next[msg.getIdBroadcaster()-1]) {
            // put this message into pending
            pending.get(msg.getIdBroadcaster()).put(msg.getSeqNum(), msg);

            // as long as there are messages that can be delivered, deliver them and remove them from pending
            LCMessage msgToDeliver = this.canDeliver();
            while (msgToDeliver != null) {
                next[msg.getIdBroadcaster() - 1]++;
                pending.get(msg.getIdBroadcaster()).remove(msgToDeliver.getSeqNum());
                this.deliver(msgToDeliver);

                msgToDeliver = this.canDeliver();
            }
        }
    }

    private void deliver(LCMessage msg) {
        logger.logDeliver(msg.getIdBroadcaster(), msg.getSeqNum());
    }

    // return a message that can be delivered if any, null otherwise
    private LCMessage canDeliver() {
        for(int idBroadcaster : pending.keySet()) {
            for (long seqNum : pending.get(idBroadcaster).keySet()) {
                boolean notBigger = true;
                for (Map.Entry<Integer, Long> pair : pending.get(idBroadcaster).get(seqNum).getClocks().entrySet()) {
                    if (pair.getValue() > next[pair.getKey() - 1]) {
                        notBigger = false;
                    }
                }
                if (notBigger) {
                    return pending.get(idBroadcaster).get(seqNum);
                }
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
