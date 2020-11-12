package cs451.communication;

import cs451.utils.Constants;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
public class UDPServer extends Thread {
    private DatagramSocket serverSocket;
    private PerfectLinks pl;

    public UDPServer(int port, PerfectLinks pl) {
        this.pl = pl;

        this.serverSocket = null;
        try {
            serverSocket = new DatagramSocket(port);
        }
        catch (SocketException e) {
            throw  new RuntimeException("Error creating UDP socket: " + e.getMessage());
        }
    }

    public void run() {
        while (true) {
            byte[] inputBuffer = new byte[Constants.BUFFER_SIZE];

            DatagramPacket inputPacket = new DatagramPacket(inputBuffer, inputBuffer.length);

            try {
                serverSocket.receive(inputPacket);
                //long recTime = System.currentTimeMillis();

                byte[] payload = inputPacket.getData();

                PLMessage msg = PLMessage.getPLMessageFromUdpPayload(payload);

                pl.udpDeliver(msg, -1);
            }
            catch (IOException e) {
                System.err.println("Error receiving datagram: " + e.getMessage());
            }
        }
    }

    public synchronized void sendDatagram(byte [] payload, InetAddress recipientIp, int recipientPort, long recTime) {
        DatagramPacket packet = new DatagramPacket(payload, payload.length, recipientIp, recipientPort);
        try {
            serverSocket.send(packet);
            /*
            if(recTime != -1) {
                System.out.println("IT TOOK " + (System.currentTimeMillis()-recTime) + " TO SEND ACK (" + pl.thisHostId + ")");
            }
            */
        } catch (IOException e) {
            System.err.println("Error sending datagram: " + e.getMessage());
        }
    }

    public DatagramSocket getServerSocket() {
        return serverSocket;
    }
}
