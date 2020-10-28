package cs451.communication;

import cs451.parser.HostsParser;
import cs451.utils.Constants;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;

public class UDPServer extends Thread {
    private int thisHostId;
    private DatagramSocket serverSocket;
    private PerfectLinks pl;

    public UDPServer(int thisHostId, int port, PerfectLinks pl) {
        this.thisHostId = thisHostId;
        this.pl = pl;

        this.serverSocket = null;
        try {
            serverSocket = new DatagramSocket(port);
        }
        catch (SocketException e) {
            System.err.println(e.getStackTrace());
            throw  new RuntimeException("Error creating UDP socket");
        }
    }

    public void run() {
        while (true) {
            byte[] inputBuffer = new byte[Constants.BUFFER_SIZE];

            DatagramPacket inputPacket = new DatagramPacket(inputBuffer, inputBuffer.length);

            try {
                serverSocket.receive(inputPacket);
            }
            catch (IOException e) {
                System.err.println("Error receiving datagram: " + e.getMessage());
            }

            byte[] payload = inputPacket.getData();
            int port = inputPacket.getPort();
            InetAddress address = inputPacket.getAddress();

            PLMessage msg = PLMessage.getPLMessageFromUdpPayload(port, address, thisHostId, payload);

            pl.udpDeliver(msg);
        }
    }

    public synchronized void sendDatagram(byte [] payload, InetAddress recipientIp, int recipientPort) {
        DatagramPacket packet = new DatagramPacket(payload, payload.length, recipientIp, recipientPort);

        try {
            serverSocket.send(packet);
        } catch (IOException e) {
            System.err.println("Error sending datagram: " + e.getStackTrace());
        }
    }

    public DatagramSocket getServerSocket() {
        return serverSocket;
    }
}
