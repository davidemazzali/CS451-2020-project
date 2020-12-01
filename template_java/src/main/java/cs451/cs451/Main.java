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
    // limit on the difference between the number of messages broadcast but not delivered yet
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

        // default number of messages in case no config is provided
        long numMsg = 10;
        ArrayList<Integer> depending = new ArrayList<>();
        if (parser.hasConfig()) {
            //System.out.println("Config: " + parser.config());
            LongArrayPair params = readNumMsg(parser.config(), id); // read the number of messages to broadcast
            numMsg = params.numMsg;
            depending = params.depending;
            //System.out.println("("+id+") "+numMsg);
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
        TopLevelBroadcast broadcaster = new LocalizedCausalBroadcast(thisHost.getId(), thisHost.getPort(), (ArrayList)parser.hosts(), depending, logger);

        Main.initSignalHandlers(logger);

        //System.out.println("Waiting for all processes for finish initialization");
        // waiting for all processes to finish initialization
        coordinator.waitOnBarrier();

        //System.out.println("Broadcasting messages...");

        int numHosts = parser.hosts().size(); // number of hosts in the network
        // begin FIFO-broadcasting messages
        for(long i = 0; i < numMsg; i++) {
            broadcaster.broadcast();

            // get from logger how many of the messages broadcast by this process it has also delivered
            long tempOwnDel = logger.accessOwnDeliveredMsgs(Logger.GET);
            // get from logger the number of broadcast messages
            long tempNumBroad = logger.accessBroadcastMsgs(Logger.GET);

            if(tempNumBroad - tempOwnDel > MSGS_PER_ROUND) {
                synchronized (logger) {
                    // then wait for the logger to tell when the remaining ones are delivered
                    //System.out.println("("+id+") waiting");
                    logger.wait();
                    //System.out.println("("+id+") exited");
                }
            }
        }
        
        //System.out.println("(" + thisHost.getId() + ") Signaling end of broadcasting messages");
        // signalling end of broadcasting messages
        coordinator.finishedBroadcasting();

        while (true) {
            // Sleep for 1 hour
            Thread.sleep(60 * 60 * 1000);
        }
    }

    private static LongArrayPair readNumMsg(String path, int thisHostId) {
        // read the number of messages to broadcast

        long numMsg = 0;
        File file = new File(path);
        Scanner sc = null;
        try {
            sc = new Scanner(file);

            if(sc.hasNext()) {
                numMsg = Integer.parseInt(sc.nextLine());
                //System.out.println(numMsg);
            }

            String line = sc.nextLine();
            String [] tokens = line.split(" ");
            while(Integer.parseInt(tokens[0]) != thisHostId) {
                line = sc.nextLine();
                tokens = line.split(" ");
            }

            ArrayList<Integer> depending = new ArrayList<>();
            for(int i = 1; i < tokens.length; i++) {
                depending.add(Integer.parseInt(tokens[i]));
            }

            sc.close();

            LongArrayPair ret = new LongArrayPair();
            ret.numMsg = numMsg;
            ret.depending = depending;

            return  ret;
        } catch (FileNotFoundException e) {
            System.err.println("Error opening config file: " + e.getMessage());
        }
        return null;
    }

    private static class LongArrayPair {
        public long numMsg;
        public ArrayList<Integer> depending;
    }
}
