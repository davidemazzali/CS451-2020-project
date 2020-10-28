package cs451.communication;

import cs451.utils.Host;
import cs451.utils.Logger;

import java.util.ArrayList;

public class BestEffortBroadcast {
    /*
    private PerfectLinks pl;

    private int nextSeqNum;
    private int thisHostId;
    private ArrayList<Host> hosts;


    private Logger logger;

    public BestEffortBroadcast(int thisHostId, int port, ArrayList<Host> hosts, Logger logger) {
        this.nextSeqNum = 0;

        this.thisHostId = thisHostId;

        this.hosts = hosts;

        this.logger = logger;

        pl = new PerfectLinks(thisHostId, port, this, logger);
    }

    public void broadcast(byte [] payload) {
        BEBMessage msg = new BEBMessage(this.getNextSeqNum(), thisHostId, payload);

        byte [] plPayload = BEBMessage.getPLPayloadFromBEBMessage(msg);
        for(Host host : hosts) {
            if(host.getId() != thisHostId) {
                pl.send(plPayload, host.getId());
            }
        }

        logger.logBroadcast(msg.getSeqNum());
    }

    public void plDeliver(BEBMessage msg) {
        this.deliver(msg);
    }

    private void  deliver(BEBMessage msg) {
        logger.logDeliver(msg.getIdSender(), msg.getSeqNum());
    }

    private int getNextSeqNum() {
        int seqNum = nextSeqNum;
        nextSeqNum++;
        return seqNum;
    }
    */
}
