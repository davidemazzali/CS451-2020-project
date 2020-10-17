package cs451;

import cs451.communication.UDPSendService;
import cs451.communication.UDPServer;
import cs451.parser.HostsParser;
import cs451.parser.Parser;
import cs451.utils.Constants;
import cs451.utils.Coordinator;
import cs451.utils.Host;

import java.net.*;
import java.nio.ByteBuffer;

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
        int id = parser.myId();

        initSignalHandlers();

        // example
        long pid = ProcessHandle.current().pid();
        System.out.println("My PID is " + pid + ".");
        System.out.println("Use 'kill -SIGINT " + pid + " ' or 'kill -SIGTERM " + pid + " ' to stop processing packets.");

        System.out.println("My id is " + id + ".");
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

        HostsParser hosts = parser.getHostsParser();
        Host thisHost = hosts.getHostById(id);

        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(thisHost.getPort());
        }
        catch (SocketException e) {
            System.err.println(e.getStackTrace());
            throw  new RuntimeException("Error creating UDP socket");
        }

        UDPServer udpServer = new UDPServer(socket);
        udpServer.start();

        System.out.println("Waiting for all processes for finish initialization");
            coordinator.waitOnBarrier();

        System.out.println("Broadcasting messages...");

        Host recipient = hosts.getHostById(((id-1)+1) % parser.hosts().size() + 1);
        System.out.println("recipient port: " + recipient.getPort());
        int numMsg = 5;
        for(int i = 0; i < numMsg; i++) {
            //UDPSendService.sendDatagram(socket, ByteBuffer.allocate(Constants.INT_BYTES_SIZE).putInt(i).array(), recipient.getIpInet(), recipient.getPort());
            UDPSendService.sendDatagram(socket, (i + " " + id).getBytes(), recipient.getIpInet(), recipient.getPort());
        }

        System.out.println("Signaling end of broadcasting messages");
            coordinator.finishedBroadcasting();

        while (true) {
            // Sleep for 1 hour
            Thread.sleep(60 * 60 * 1000);
        }
    }
}
