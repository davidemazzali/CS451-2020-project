package cs451.communication;

import cs451.utils.Constants;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UDPServer extends Thread{
    private DatagramSocket serverSocket;

    public UDPServer(DatagramSocket serverSocket) {
        this.serverSocket = serverSocket;
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

            System.out.println(new String(payload) + " AAAAAAAAAAAAAAAAAAAAAAA");
        }
    }

    public DatagramSocket getServerSocket() {
        return serverSocket;
    }
}
