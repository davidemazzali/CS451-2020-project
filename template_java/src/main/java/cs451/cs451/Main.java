package cs451;

import cs451.parser.Parser;
import cs451.utils.Coordinator;
import cs451.utils.Host;

import java.io.IOException;
import java.net.*;

public class Main {

    private static void handleSignal() {
        //immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");

        //write/flush output file if necessary
        System.out.println("Writing output.");
    }

    private static void initSignalHandlers() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                handleSignal();
            }
        });
    }

    public static void main(String[] args) throws InterruptedException {
        Parser parser = new Parser(args);
        parser.parse();

        initSignalHandlers();

        // example
        long pid = ProcessHandle.current().pid();
        System.out.println("My PID is " + pid + ".");
        System.out.println("Use 'kill -SIGINT " + pid + " ' or 'kill -SIGTERM " + pid + " ' to stop processing packets.");

        System.out.println("My id is " + parser.myId() + ".");
        System.out.println("List of hosts is:");
        for (Host host: parser.hosts()) {
            System.out.println(host.getId() + ", " + host.getIp() + ", " + host.getPort());
        }

        System.out.println("Barrier: " + parser.barrierIp() + ":" + parser.barrierPort());
        System.out.println("Signal: " + parser.signalIp() + ":" + parser.signalPort());
        System.out.println("Output: " + parser.output());
        // if config is defined; always check before parser.config()
        if (parser.hasConfig()) {
            System.out.println("Config: " + parser.config());
        }


        Coordinator coordinator = new Coordinator(parser.myId(), parser.barrierIp(), parser.barrierPort(), parser.signalIp(), parser.signalPort());

        System.out.println("Waiting for all processes for finish initialization");
            coordinator.waitOnBarrier();

        System.out.println("Broadcasting messages...");

        int serverPort, targetPort;

        if(parser.myId() == 1) {
            try {
                serverPort = 11001;
                // Instantiate a new DatagramSocket to receive responses from the client
                DatagramSocket serverSocket = new DatagramSocket(serverPort);
            
            /* Create buffers to hold sending and receiving data.
            It temporarily stores data in case of communication delays */
                byte[] receivingDataBuffer = new byte[1024];
                byte[] sendingDataBuffer = new byte[1024];
            
            /* Instantiate a UDP packet to store the 
            client data using the buffer for receiving data*/
                DatagramPacket inputPacket = new DatagramPacket(receivingDataBuffer, receivingDataBuffer.length);
                System.out.println("Waiting for a client to connect...");

                // Receive data from the client and store in inputPacket
                serverSocket.receive(inputPacket);

                // Printing out the client sent data
                String receivedData = new String(inputPacket.getData());
                System.out.println("Sent from the client: " + receivedData);

                /*
                 * Convert client sent data string to upper case,
                 * Convert it to bytes
                 *  and store it in the corresponding buffer. */
                sendingDataBuffer = receivedData.toUpperCase().getBytes();

                // Obtain client's IP address and the port
                InetAddress senderAddress = inputPacket.getAddress();
                int senderPort = inputPacket.getPort();

                // Create new UDP packet with data to send to the client
                DatagramPacket outputPacket = new DatagramPacket(
                        sendingDataBuffer, sendingDataBuffer.length,
                        senderAddress, senderPort
                );

                // Send the created packet to client
                serverSocket.send(outputPacket);
                // Close the socket connection
                serverSocket.close();
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            try {
                serverPort = 11002;
                targetPort = 11001;
                // Instantiate a new DatagramSocket to receive responses from the client
                DatagramSocket serverSocket = new DatagramSocket(serverPort);

            /* Create buffers to hold sending and receiving data.
            It temporarily stores data in case of communication delays */
                byte[] receivingDataBuffer = new byte[1024];
                byte[] sendingDataBuffer = new byte[1024];

                System.out.println("Scrivo a quel'altro");

                sendingDataBuffer = "AO vediamo se funziona".getBytes();

                InetAddress targetAddress = InetAddress.getByName("127.0.0.1");

                // Create new UDP packet with data to send to the client
                DatagramPacket outputPacket = new DatagramPacket(
                        sendingDataBuffer, sendingDataBuffer.length,
                        targetAddress, targetPort
                );

                // Send the created packet to client
                serverSocket.send(outputPacket);

                DatagramPacket inputPacket = new DatagramPacket(receivingDataBuffer, receivingDataBuffer.length);
                System.out.println("Waiting for a client to connect...");

                // Receive data from the client and store in inputPacket
                serverSocket.receive(inputPacket);

                // Printing out the client sent data
                String receivedData = new String(inputPacket.getData());
                System.out.println("Sent from quel'altro: " + receivedData);

                // Close the socket connection
                serverSocket.close();
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Signaling end of broadcasting messages");
            coordinator.finishedBroadcasting();

        while (true) {
            // Sleep for 1 hour
            Thread.sleep(60 * 60 * 1000);
        }
    }
}
