package cs451;

import cs451.communication.*;
import cs451.parser.HostsParser;
import cs451.parser.Parser;
import cs451.utils.Coordinator;
import cs451.utils.Host;
import cs451.utils.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {

    private static void handleSignal(Logger logger) {
        //immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");

        //write/flush output file if necessary
        System.out.println("Writing output.");

        logger.logToOutFile();
    }

    private static void initSignalHandlers(Logger logger) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                handleSignal(logger);
            }
        });
    }

    public static void main(String[] args) throws InterruptedException {
        Parser parser = new Parser(args);
        parser.parse();
        int id = parser.myId();

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

        Logger logger = new Logger(parser.output());
        UniformReliableBroadcast urb = new UniformReliableBroadcast(thisHost.getId(), thisHost.getPort(), (ArrayList)parser.hosts(), logger);

        Main.initSignalHandlers(logger);

        System.out.println("Waiting for all processes for finish initialization");
        coordinator.waitOnBarrier();

        System.out.println("Broadcasting messages...");

        int numMsg = readNumMsg(parser.config());
        for(int i = 0; i < numMsg; i++) {
            urb.broadcast();
        }

        
        System.out.println("Signaling end of broadcasting messages");
        coordinator.finishedBroadcasting();

        while (true) {
            // Sleep for 1 hour
            Thread.sleep(60 * 60 * 1000);
        }
    }

    private static int readNumMsg(String path) {
        int numMsg = 0;
        File file = new File(path);
        Scanner sc = null;
        try {
            sc = new Scanner(file);

            if(sc.hasNext()) {
                numMsg = sc.nextInt();
            }

            return  numMsg;
        } catch (FileNotFoundException e) {
            System.err.println("Error opening config file: " + e.getMessage());
        }
        return numMsg;
    }
}
