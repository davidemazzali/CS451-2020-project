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
import java.util.Collection;
import java.util.Collections;
import java.util.Scanner;

public class Main {
    public static final int MSGS_PER_ROUND = 100;

    private static void handleSignal(Logger logger) {
        //immediately stop network packet processing
        logger.accessInterrupted(true);
        //System.out.println("Immediately stopping network packet processing.");

        //write/flush output file if necessary
        //System.out.println("Writing output.");

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
        //System.out.println("My PID is " + pid + ".");
        //System.out.println("Use 'kill -SIGINT " + pid + " ' or 'kill -SIGTERM " + pid + " ' to stop processing packets.");

        //System.out.println("My id is " + id + ".");
        //System.out.println("List of hosts is:");
        for (Host host: parser.hosts()) {
            //System.out.println(host.getId() + ", " + host.getIp() + ", " + host.getPort());
        }

        //System.out.println("Barrier: " + parser.barrierIp() + ":" + parser.barrierPort());
        //System.out.println("Signal: " + parser.signalIp() + ":" + parser.signalPort());
        //System.out.println("Output: " + parser.output());
        // if config is defined; always check before parser.config()
        if (parser.hasConfig()) {
            //System.out.println("Config: " + parser.config());
        }

        Coordinator coordinator = new Coordinator(parser.myId(), parser.barrierIp(), parser.barrierPort(), parser.signalIp(), parser.signalPort());

        HostsParser hosts = parser.getHostsParser();
        Collections.sort(hosts.getHosts());

        Host thisHost = hosts.getHostById(id);

        int numMsg = readNumMsg(parser.config());
        Logger logger = new Logger(parser.output(), hosts.getHosts().size(), numMsg, thisHost.getId());
        FIFOBroadcast fifo = new FIFOBroadcast(thisHost.getId(), thisHost.getPort(), (ArrayList)parser.hosts(), logger);
        //UniformReliableBroadcast urb = new UniformReliableBroadcast(thisHost.getId(), thisHost.getPort(), (ArrayList)parser.hosts(), null, logger);

        Main.initSignalHandlers(logger);

        //System.out.println("Waiting for all processes for finish initialization");
        coordinator.waitOnBarrier();

        //System.out.println("Broadcasting messages...");

        //int numMsg = readNumMsg(parser.config());
        int numHosts = parser.hosts().size();
        for(int i = 0; i < numMsg; i++) {
            //System.out.println("(" + thisHost.getId() + ") broadcast message (" + i + ")");
            //urb.broadcast(null);
            fifo.broadcast();

            if((i+1) % MSGS_PER_ROUND == 0) {
                int tempOwnDel = logger.accessOwnDeliveredMsgs(Logger.GET);
                int tempNumBroad = logger.accessBroadcastMsgs(Logger.GET);

                if(tempOwnDel < tempNumBroad) {
                    synchronized (logger) {
                        logger.wait();
                    }
                }
            }

            /*
            if(tempNumDel <= Math.floor(((double)tempNumBroad)/2.0)) {
                synchronized (logger) {
                    logger.wait();
                }
            }
             */
        }
        
        //System.out.println("(" + thisHost.getId() + ") Signaling end of broadcasting messages");
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
