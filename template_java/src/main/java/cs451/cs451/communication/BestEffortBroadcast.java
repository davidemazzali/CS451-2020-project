package cs451.communication;

import cs451.utils.Host;
import cs451.utils.Logger;

import java.util.ArrayList;

public class BestEffortBroadcast {
    private UniformReliableBroadcast urb;
    private PerfectLinks pl;

    private int nextSeqNum;
    private int thisHostId;
    //private ArrayList<Host> hosts;
    private ArrayList<Host> neighbours;

    private Logger logger;

    public BestEffortBroadcast(int thisHostId, int port, ArrayList<Host> hosts, UniformReliableBroadcast urb, Logger logger) {
        this.nextSeqNum = 0;
        this.thisHostId = thisHostId;

        neighbours = new ArrayList<>();
        if(hosts.size() >= 4) {
            if (hosts.size() % 2 == 1) {
                int i = 1;
                boolean overed = false;
                while (!overed) {
                    if(i % 2 == 0) {
                        neighbours.add(hosts.get(((thisHostId - 1) + i) % hosts.size()));
                    }

                    if(i > hosts.size()) {
                        overed = true;
                    }
                    i++;
                }
            }
            else {
                int i = 1;
                boolean overed = false;
                while (!overed) {
                    if(i % 2 == 0) {
                        neighbours.add(hosts.get(((thisHostId - 1) + i) % hosts.size()));
                    }

                    if(i == hosts.size()) {
                        overed = true;
                    }
                    i++;
                }
                neighbours.add(hosts.get(((thisHostId - 1) + 1) % hosts.size()));
            }
        }
        else {
            neighbours = hosts;
        }

        this.logger = logger;

        this.urb = urb;
        pl = new PerfectLinks(thisHostId, port, this, logger);
    }

    public void broadcast(URBMessage payload) {
        BEBMessage msg = new BEBMessage(this.getNextSeqNum(), thisHostId, payload);

        for(Host host : neighbours) {
            pl.send(msg, host.getId());
        }

        //logger.logBroadcast(msg.getSeqNum());
    }

    public synchronized void plDeliver(BEBMessage msg) {
        this.deliver(msg);
    }

    private void deliver(BEBMessage msg) {
        //logger.logDeliver(msg.getIdSender(), msg.getSeqNum());
        urb.bebDeliver(msg.getPayload());
    }

    private int getNextSeqNum() {
        int seqNum = nextSeqNum;
        nextSeqNum++;
        return seqNum;
    }
}
