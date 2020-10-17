package cs451.communication;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPSendService {
    public static synchronized void sendDatagram(DatagramSocket senderSocket, byte [] payload, InetAddress recipientIp, int recipientPort) {
        DatagramPacket packet = new DatagramPacket(payload, payload.length, recipientIp, recipientPort);

        try {
            senderSocket.send(packet);
        } catch (IOException e) {
            System.err.println("Error sending datagram: " + e.getStackTrace());
        }
    }
}
