package cs451.communication;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import static cs451.utils.Constants.BUFFER_SIZE;

public class UDPServer {
    InetAddress serverAddress;
    int serverPort;

    public UDPServer(InetAddress serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    public void startListening() throws IOException {
        DatagramSocket serverSocket = new DatagramSocket(serverPort);

        byte[] inputBuffer = new byte[BUFFER_SIZE];

        DatagramPacket inputPacket = new DatagramPacket(inputBuffer, inputBuffer.length);

        while(true) {
            serverSocket.receive(inputPacket);

            byte [] payload = inputPacket.getData();


        }
    }
}
