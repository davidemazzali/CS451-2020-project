package cs451;

import cs451.communication.BEBMessage;
import cs451.communication.BestEffortBroadcast;
import cs451.communication.PLMessage;
import cs451.communication.PerfectLinks;
import cs451.parser.HostsParser;
import cs451.parser.Parser;
import cs451.utils.Coordinator;
import cs451.utils.Host;
import cs451.utils.Logger;

import java.util.ArrayList;
import java.util.Random;

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
        //BestEffortBroadcast beb = new BestEffortBroadcast(thisHost.getId(), thisHost.getPort(), (ArrayList) parser.hosts(), logger);
        PerfectLinks pl = new PerfectLinks(thisHost.getId(), thisHost.getPort(), logger);

        Main.initSignalHandlers(logger);

        System.out.println("Waiting for all processes for finish initialization");
            coordinator.waitOnBarrier();

        System.out.println("Broadcasting messages...");

        int numMsg = 1;
        for(int i = 0; i < numMsg; i++) {
            BEBMessage msg = new BEBMessage(i, id, "hello world".getBytes());
            pl.send(BEBMessage.getPLPayloadFromBEBMessage(msg), id == 1 ? 2 : 1);
        }

        /*
        int numMsg = 1;
        for(int i = 0; i < numMsg; i++) {
            beb.broadcast("hello world".getBytes());
        }
        */

        System.out.println("Signaling end of broadcasting messages");
            coordinator.finishedBroadcasting();

        while (true) {
            // Sleep for 1 hour
            Thread.sleep(60 * 60 * 1000);
        }
    }
}
