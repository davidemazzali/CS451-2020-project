package cs451.communication;

import cs451.utils.Host;
import cs451.utils.Logger;

import java.util.ArrayList;

public class BestEffortBroadcast {
    private UniformReliableBroadcast urb;
    private PerfectLinks pl;

    private long nextSeqNum;
    private int thisHostId;
    private ArrayList<Host> hosts;

    private Logger logger;

    public BestEffortBroadcast(int thisHostId, int port, ArrayList<Host> hosts, UniformReliableBroadcast urb, Logger logger) {
        this.nextSeqNum = 0;
        this.thisHostId = thisHostId;

        this.hosts = new ArrayList<>();
        this.hosts = hosts;

        this.logger = logger;

        this.urb = urb;
        pl = new PerfectLinks(thisHostId, port, this, logger);
    }

    public void broadcast(URBMessage payload, boolean fromFifo) {
        BEBMessage msg = new BEBMessage(this.getNextSeqNum(), thisHostId, payload);

        if(fromFifo) {
            // log the broadcast if this method was invoked by FIFO (i.e. this is not a URB relay)
            logger.logBroadcast(payload.getPayload().getSeqNum());
        }
        // send to all hosts
        for(Host host : hosts) {
            if(host.getId() != thisHostId) {
                pl.send(msg, host.getId());
            }
        }
        pl.send(msg, thisHostId);
    }

    public synchronized void plDeliver(BEBMessage msg) {
        this.deliver(msg);
    }

    private void deliver(BEBMessage msg) {
        urb.bebDeliver(msg.getPayload());
    }

    // get next sequence number and increment
    private long getNextSeqNum() {
        long seqNum = nextSeqNum;
        nextSeqNum++;
        return seqNum;
    }
}
