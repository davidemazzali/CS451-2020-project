package cs451.communication;

import cs451.utils.Host;
import cs451.utils.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UniformReliableBroadcast {
    private BestEffortBroadcast beb;

    private int nextSeqNum;
    private int thisHostId;
    private HashMap<Integer, HashMap<Integer, URBMessage>> delivered;
    private HashMap<Integer, HashMap<Integer, URBMessage>> pending;
    private HashMap<Integer, HashMap<Integer, Integer>> acks;
    private ArrayList<Host> hosts;

    private Logger logger;

    public UniformReliableBroadcast(int thisHostId, int port, ArrayList<Host> hosts, Logger logger) {
        nextSeqNum = 0;
        this.thisHostId = thisHostId;
        this.hosts = hosts;
        delivered = new HashMap<>();
        pending = new HashMap<>();
        acks = new HashMap<>();

        this.logger = logger;

        beb =  new BestEffortBroadcast(thisHostId, port, hosts, this, logger);
    }

    public void broadcast() {
        URBMessage msg = new URBMessage(this.getNextSeqNum(), thisHostId);

        if(!pending.containsKey(thisHostId)) {
            pending.put(thisHostId, new HashMap<>());
        }
        pending.get(thisHostId).put(msg.getSeqNum(), msg);

        beb.broadcast(msg);

        logger.logBroadcast(msg.getSeqNum());
    }

    public synchronized void bebDeliver(int senderId, URBMessage msg) {
        if(!acks.containsKey(msg.getIdBroadcaster())) {
            acks.put(msg.getIdBroadcaster(), new HashMap<>());
        }
        if(!acks.get(msg.getIdBroadcaster()).containsKey(msg.getSeqNum())) {
            acks.get(msg.getIdBroadcaster()).put(msg.getSeqNum(), 0);
        }
        acks.get(msg.getIdBroadcaster()).put(msg.getSeqNum(), acks.get(msg.getIdBroadcaster()).get(msg.getSeqNum()) + 1);

        if(!pending.containsKey(msg.getIdBroadcaster()) || !pending.get(msg.getIdBroadcaster()).containsKey(msg.getSeqNum())) {
            if(!pending.containsKey(msg.getIdBroadcaster())) {
                pending.put(msg.getIdBroadcaster(), new HashMap<>());
            }
            pending.get(msg.getIdBroadcaster()).put(msg.getSeqNum(), msg);

            beb.broadcast(msg);
        }

        checkDeliver();
    }

    private void deliver(URBMessage msg) {
        if(!delivered.containsKey(msg.getIdBroadcaster())) {
            delivered.put(msg.getIdBroadcaster(), new HashMap<>());
        }
        delivered.get(msg.getIdBroadcaster()).put(msg.getSeqNum(), msg);

        logger.logDeliver(msg.getIdBroadcaster(), msg.getSeqNum());
    }

    private synchronized void checkDeliver() {
        for(HashMap<Integer, URBMessage> msgsFromHost : pending.values()) {
            for(URBMessage msg : msgsFromHost.values()) {
                if(canDeliver(msg)) {
                    if(!delivered.containsKey(msg.getIdBroadcaster()) || !delivered.get(msg.getIdBroadcaster()).containsKey(msg.getSeqNum())) {
                        this.deliver(msg);
                    }
                }
            }
        }
    }

    private boolean canDeliver(URBMessage msg) {
        boolean res;
        if(!acks.containsKey(msg.getIdBroadcaster()) || !acks.get(msg.getIdBroadcaster()).containsKey(msg.getSeqNum())) {
            res = false;
        }
        else {
            res = acks.get(msg.getIdBroadcaster()).get(msg.getSeqNum()) > hosts.size()/2;
        }
        return res;
    }

    private int getNextSeqNum() {
        int seqNum = nextSeqNum;
        nextSeqNum++;
        return seqNum;
    }
}
