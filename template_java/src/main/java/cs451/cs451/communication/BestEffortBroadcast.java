package cs451.communication;

import cs451.utils.Host;
import cs451.utils.Logger;

import java.util.ArrayList;

public class BestEffortBroadcast {
    private UniformReliableBroadcast urb;
    private PerfectLinks pl;

    private int nextSeqNum;
    private int thisHostId;
    private ArrayList<Host> hosts;

    private Logger logger;

    public BestEffortBroadcast(int thisHostId, int port, ArrayList<Host> hosts, UniformReliableBroadcast urb, Logger logger) {
        this.nextSeqNum = 0;
        this.thisHostId = thisHostId;
        this.hosts = hosts;

        this.logger = logger;

        this.urb = urb;
        pl = new PerfectLinks(thisHostId, port, this, logger);
    }

    public void broadcast(URBMessage payload) {
        BEBMessage msg = new BEBMessage(this.getNextSeqNum(), thisHostId, payload);

        for(Host host : hosts) {
            pl.send(msg, host.getId());
        }

        //logger.logBroadcast(msg.getSeqNum());
    }

    public synchronized void plDeliver(BEBMessage msg) {
        this.deliver(msg);
    }

    private void deliver(BEBMessage msg) {
        //logger.logDeliver(msg.getIdSender(), msg.getSeqNum());
        urb.bebDeliver(msg.getIdSender(), msg.getPayload());
    }

    private int getNextSeqNum() {
        int seqNum = nextSeqNum;
        nextSeqNum++;
        return seqNum;
    }
}
