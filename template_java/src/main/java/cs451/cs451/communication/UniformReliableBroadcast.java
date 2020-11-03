package cs451.communication;

import cs451.utils.Host;
import cs451.utils.Logger;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UniformReliableBroadcast {
    private BestEffortBroadcast beb;
    private FIFOBroadcast fifo;

    private int nextSeqNum;
    private int thisHostId;
    private HashMap<Integer, HashMap<Integer, URBMessage>> delivered;
    private HashMap<Integer, HashMap<Integer, URBMessage>> pending;
    private HashMap<Integer, HashMap<Integer, Integer>> acks;
    private int numHosts;

    private Logger logger;

    private static final int GET_ALL = 0;
    private static final int PUT = 1;

    public UniformReliableBroadcast(int thisHostId, int port, ArrayList<Host> hosts, FIFOBroadcast fifo, Logger logger) {
        nextSeqNum = 0;
        this.thisHostId = thisHostId;
        this.numHosts = hosts.size();
        delivered = new HashMap<>();
        pending = new HashMap<>();
        acks = new HashMap<>();

        this.logger = logger;

        this.fifo = fifo;
        beb =  new BestEffortBroadcast(thisHostId, port, hosts, this, logger);
    }

    public void broadcast(FIFOMessage payload) {
        URBMessage msg = new URBMessage(this.getNextSeqNum(), thisHostId, payload);

        accessPending(PUT, msg);

        beb.broadcast(msg);

        //logger.logBroadcast(msg.getSeqNum());
    }

    public synchronized void bebDeliver(URBMessage msg) {
        if(!acks.containsKey(msg.getIdBroadcaster())) {
            acks.put(msg.getIdBroadcaster(), new HashMap<>());
        }
        if(!acks.get(msg.getIdBroadcaster()).containsKey(msg.getSeqNum())) {
            acks.get(msg.getIdBroadcaster()).put(msg.getSeqNum(), 0);
        }
        acks.get(msg.getIdBroadcaster()).put(msg.getSeqNum(), acks.get(msg.getIdBroadcaster()).get(msg.getSeqNum()) + 1);

        if((Boolean)accessPending(PUT, msg)) {
            beb.broadcast(msg);
        }

        checkDeliver();
    }

    private void checkDeliver() {
        for(HashMap<Integer, URBMessage> msgsFromHost : (ArrayList<HashMap<Integer, URBMessage>>)accessPending(GET_ALL, null)) {
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
            res = acks.get(msg.getIdBroadcaster()).get(msg.getSeqNum()) > numHosts/2;
        }
        return res;
    }

    private void deliver(URBMessage msg) {
        if(!delivered.containsKey(msg.getIdBroadcaster())) {
            delivered.put(msg.getIdBroadcaster(), new HashMap<>());
        }
        delivered.get(msg.getIdBroadcaster()).put(msg.getSeqNum(), msg);

        //logger.logDeliver(msg.getIdBroadcaster(), msg.getSeqNum());
        fifo.urbDeliver(msg.getPayload());
    }

    private synchronized Object accessPending(int op, URBMessage msg) {
        switch(op) {
            case GET_ALL:
                ArrayList<HashMap<Integer, URBMessage>> values = new ArrayList<>();
                int i = 0;
                for(HashMap<Integer, URBMessage> msgsFromHost : pending.values()) {
                    values.add(new HashMap<>());
                    for(Map.Entry<Integer, URBMessage> entry : msgsFromHost.entrySet()) {
                        values.get(i).put(entry.getKey(), entry.getValue());
                    }
                    i++;
                }
                return values;
            case PUT:
                if(!pending.containsKey(msg.getIdBroadcaster()) || !pending.get(msg.getIdBroadcaster()).containsKey(msg.getSeqNum())) {
                    if(!pending.containsKey(msg.getIdBroadcaster())) {
                        pending.put(msg.getIdBroadcaster(), new HashMap<>());
                    }
                    pending.get(msg.getIdBroadcaster()).put(msg.getSeqNum(), msg);

                    return true;
                }
                else {
                    return false;
                }
            default:
                return null;
        }
    }

    private int getNextSeqNum() {
        int seqNum = nextSeqNum;
        nextSeqNum++;
        return seqNum;
    }
}
