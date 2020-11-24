package cs451.communication;

import cs451.utils.Constants;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class PLMessage implements Serializable{
    private long seqNum;
    private int idSender;
    private int idRecipient;
    private BEBMessage payload;
    private long seqNumToAck;

    public PLMessage(long seqNum, int idSender, int idRecipient, BEBMessage payload) {
        this.seqNum = seqNum;
        this.idSender = idSender;
        this.idRecipient = idRecipient;
        this.payload = payload;
        this.seqNumToAck = -1;
    }

    public PLMessage(long seqNum, int idSender, int idRecipient, long seqNumToAck) {
        this.seqNum = seqNum;
        this.idSender = idSender;
        this.idRecipient = idRecipient;
        this.payload = null;
        this.seqNumToAck = seqNumToAck;
    }

    public long getSeqNum() {
        return seqNum;
    }

    public int getIdSender() {
        return idSender;
    }

    public int getIdRecipient() {
        return idRecipient;
    }

    public BEBMessage getPayload() {
        return payload;
    }

    public Long getSeqNumToAck() {
        return seqNumToAck;
    }

    // convert from PLMessage to byte array
    public static byte [] getUdpPayloadFromPLMessage(PLMessage msg) {
        int length = 60+4;
        if(msg.payload != null) {
            if(msg.getPayload().getPayload().getPayload() instanceof LCMessage) {
                length += 4;
                length += (4+8)*((LCMessage) msg.getPayload().getPayload().getPayload()).getClocks().size();
            }
        }
        ByteBuffer tempBuffer = ByteBuffer.allocate(length);
        tempBuffer.putLong(0, msg.seqNum);
        tempBuffer.putInt(8, msg.idRecipient);
        tempBuffer.putInt(12, msg.idSender);
        tempBuffer.putLong(16, msg.seqNumToAck);
        if(msg.payload != null) {
            tempBuffer.putLong(24, msg.getPayload().getSeqNum());
            tempBuffer.putInt(32, msg.getPayload().getIdSender());
            tempBuffer.putLong(36, msg.getPayload().getPayload().getSeqNum());
            tempBuffer.putInt(44, msg.getPayload().getPayload().getIdBroadcaster());
            tempBuffer.putLong(48, msg.getPayload().getPayload().getPayload().getSeqNum());
            tempBuffer.putInt(56, msg.getPayload().getPayload().getPayload().getIdBroadcaster());
            if(msg.getPayload().getPayload().getPayload() instanceof LCMessage) {
                tempBuffer.putInt(60, 1);
                HashMap<Integer, Long> clocks = ((LCMessage) msg.getPayload().getPayload().getPayload()).getClocks();
                tempBuffer.putInt(64, clocks.size());
                int i = 0;
                for(Map.Entry<Integer, Long> entry : clocks.entrySet()) {
                    tempBuffer.putInt(68 + i*(4+8), entry.getKey());
                    tempBuffer.putLong(68 + i*(4+8) + 4, entry.getValue());
                    i++;
                }
            }
            else {
                //tempBuffer.putInt(60, 2);
            }
        }
        else {
            tempBuffer.putLong(24, -1);
        }

        return tempBuffer.array();
    }

    // convert from byte array to PLMessage
    public static synchronized PLMessage getPLMessageFromUdpPayload(byte [] udpPayload) {

        PLMessage msg = null;

        ByteBuffer tempBuffer = ByteBuffer.allocate(Constants.BUFFER_SIZE);
        tempBuffer.put(udpPayload);

        long plSeqNum =tempBuffer.getLong(0);
        int plRec =tempBuffer.getInt(8);
        int plSend =tempBuffer.getInt(12);
        long plSeqAck =tempBuffer.getLong(16);
        long bebSeqNum =tempBuffer.getLong(24);
        if(bebSeqNum != -1) {
            int bebSender = tempBuffer.getInt(32);
            long urbSeqNum = tempBuffer.getLong(36);
            int urbBroad = tempBuffer.getInt(44);
            long tlSeqNum = tempBuffer.getLong(48);
            int tlBroad = tempBuffer.getInt(56);
            int typeTl = tempBuffer.getInt(60);

            if(typeTl == 1) {
                HashMap<Integer, Long> clocks = new HashMap<>();
                int numEntries = tempBuffer.getInt(64);
                for(int i = 0; i < numEntries; i++) {
                    clocks.put(tempBuffer.getInt(68 + i*(4+8)), tempBuffer.getLong(68 + i*(4+8)+4));
                }

                msg = new PLMessage(plSeqNum, plSend, plRec,
                        new BEBMessage(bebSeqNum, bebSender,
                                new URBMessage(urbSeqNum, urbBroad,
                                        new LCMessage(tlSeqNum, tlBroad, clocks)
                                )
                        )
                );
            }
            else {
                msg = new PLMessage(plSeqNum, plSend, plRec,
                        new BEBMessage(bebSeqNum, bebSender,
                                new URBMessage(urbSeqNum, urbBroad,
                                        new FIFOMessage(tlSeqNum, tlBroad)
                                )
                        )
                );
            }
        }
        else {
            msg = new PLMessage(plSeqNum, plSend, plRec, plSeqAck);
        }

        return msg;
    }
}
