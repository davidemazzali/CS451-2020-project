package cs451;

import cs451.communication.*;
import cs451.parser.HostsParser;
import cs451.parser.Parser;
import cs451.utils.Coordinator;
import cs451.utils.Host;
import cs451.utils.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

public class Main {
    // MSGS_PER_ROUND messages can be broadcast,
    // but before broadcasting others, the process has to deliver all the messages it has broadcast so far
    // (see below in main function)
    public static final int MSGS_PER_ROUND = 100;

    private static void handleSignal(Logger logger) {
        // telling the logger to ignore attempts to log new events from now on
        logger.accessInterrupted(true);
        //System.out.println("Immediately stopping network packet processing.");

        //System.out.println("Writing output.");

        // writing the events that have not been logged yet to the output file
        logger.logToOutFile();
        try {
            // closing the file writer
            logger.getWriter().close();
        } catch (IOException e) {
            System.err.println("Error closing file writer: " + e.getMessage());
        }
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
        //for (Host host: parser.hosts()) {
            //System.out.println(host.getId() + ", " + host.getIp() + ", " + host.getPort());
        //}

        //System.out.println("Barrier: " + parser.barrierIp() + ":" + parser.barrierPort());
        //System.out.println("Signal: " + parser.signalIp() + ":" + parser.signalPort());
        //System.out.println("Output: " + parser.output());
        // if config is defined; always check before parser.config()

        // default number of messages in case no config is provided
        long numMsg = 10;
        if (parser.hasConfig()) {
            //System.out.println("Config: " + parser.config());
            numMsg = readNumMsg(parser.config()); // read the number of messages to broadcast
        }

        Coordinator coordinator = new Coordinator(parser.myId(), parser.barrierIp(), parser.barrierPort(), parser.signalIp(), parser.signalPort());

        HostsParser hosts = parser.getHostsParser();
        // ensure hosts are ordered by ID
        Collections.sort(hosts.getHosts());

        // copy of this host
        Host thisHost = HostsParser.getHostById(id);

        // instantiate logger
        Logger logger = new Logger(parser.output(), hosts.getHosts().size(), numMsg, thisHost.getId());

        // instantiate the FIFO broadcast layer
        FIFOBroadcast fifo = new FIFOBroadcast(thisHost.getId(), thisHost.getPort(), (ArrayList)parser.hosts(), logger);

        Main.initSignalHandlers(logger);

        //System.out.println("Waiting for all processes for finish initialization");
        // waiting for all processes to finish initialization
        coordinator.waitOnBarrier();

        //System.out.println("Broadcasting messages...");

        int numHosts = parser.hosts().size(); // number of hosts in the network
        // begin FIFO-broadcasting messages
        for(long i = 0; i < numMsg; i++) {
            fifo.broadcast();

            // get from logger how many of the messages broadcast by this process it has also delivered
            long tempOwnDel = logger.accessOwnDeliveredMsgs(Logger.GET);
            // get from logger the number of broadcast messages
            long tempNumBroad = logger.accessBroadcastMsgs(Logger.GET);

            if(tempNumBroad - tempOwnDel > MSGS_PER_ROUND) {
                synchronized (logger) {
                    // then wait for the logger to tell when the remaining ones are delivered
                    logger.wait();
                }
            }
            /*
            if((i+1) % MSGS_PER_ROUND == 0) { // if the current round has ended
                // get from logger how many of the messages broadcast by this process it has also delivered
                long tempOwnDel = logger.accessOwnDeliveredMsgs(Logger.GET);
                // get from logger the number of broadcast messages
                long tempNumBroad = logger.accessBroadcastMsgs(Logger.GET);

                // if this process has not delivered yet some of the messages it has broadcast
                if(tempOwnDel < tempNumBroad) {
                    synchronized (logger) {
                        // then wait for the logger to tell when the remaining ones are delivered
                        logger.wait();
                    }
                }
            }
            */
        }
        
        //System.out.println("(" + thisHost.getId() + ") Signaling end of broadcasting messages");
        // signalling end of broadcasting messages
        coordinator.finishedBroadcasting();

        while (true) {
            // Sleep for 1 hour
            Thread.sleep(60 * 60 * 1000);
        }
    }

    private static long readNumMsg(String path) {
        // read the number of messages to broadcast

        long numMsg = 0;
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
