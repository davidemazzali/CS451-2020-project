package cs451.communication;

import cs451.utils.Constants;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
public class UDPServer {
    private DatagramSocket serverSocket;
    private PerfectLinks pl;

    public UDPServer(int port, PerfectLinks pl) {
        this.pl = pl;

        this.serverSocket = null;
        try {
            serverSocket = new DatagramSocket(port);
            ListenOnSocket listenOnSocket = new ListenOnSocket();
            listenOnSocket.start();
        }
        catch (SocketException e) {
            throw  new RuntimeException("Error creating UDP socket: " + e.getMessage());
        }
    }

    private class ListenOnSocket extends Thread {
        public void run() {
            // listen on socket and deliver packets to perfect link
            while (true) {
                byte[] inputBuffer = new byte[Constants.BUFFER_SIZE];

                DatagramPacket inputPacket = new DatagramPacket(inputBuffer, inputBuffer.length);

                try {
                    //System.out.println("(" + pl.thisHostId + ") waiting to receive");
                    serverSocket.receive(inputPacket);
                    //System.out.println("(" + pl.thisHostId + ") receiving AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAa");

                    byte[] payload = inputPacket.getData();

                    PLMessage msg = PLMessage.getPLMessageFromUdpPayload(payload);
                    pl.udpDeliver(msg, -1);
                } catch (IOException e) {
                    System.err.println("Error receiving datagram: " + e.getMessage());
                }
            }
        }
    }

    public synchronized void sendDatagram(byte [] payload, InetAddress recipientIp, int recipientPort, long recTime) {
        // send datagram
        DatagramPacket packet = new DatagramPacket(payload, payload.length, recipientIp, recipientPort);
        PLMessage msg = PLMessage.getPLMessageFromUdpPayload(payload);
        if(msg.getSeqNum() != -1) {
            //System.out.println("("+pl.thisHostId+") sending");
        }
        else {
            //System.out.println("("+pl.thisHostId+") acking");
        }
        try {
            serverSocket.send(packet);
        } catch (IOException e) {
            System.err.println("Error sending datagram: " + e.getMessage());
        }
    }

    public DatagramSocket getServerSocket() {
        return serverSocket;
    }
}
