package cs451.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Host implements Comparable{

    private static final String IP_START_REGEX = "/";

    private int id;
    private String ip;
    private InetAddress ipInet;
    private int port = -1;

    public boolean populate(String idString, String ipString, String portString) {
        try {
            id = Integer.parseInt(idString);


            String ipTest = InetAddress.getByName(ipString).toString();
            if (ipTest.startsWith(IP_START_REGEX)) {
                ip = ipTest.substring(1);
            } else {
                ip = InetAddress.getByName(ipTest.split(IP_START_REGEX)[0]).getHostAddress();
            }
            ipInet = InetAddress.getByName(ip);

            port = Integer.parseInt(portString);
            if (port <= 0) {
                System.err.println("Port in the hosts file must be a positive number!");
                return false;
            }
        } catch (NumberFormatException e) {
            if (port == -1) {
                System.err.println("Id in the hosts file must be a number!");
            } else {
                System.err.println("Port in the hosts file must be a number!");
            }
            return false;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        return true;
    }

    public int getId() {
        return id;
    }

    public String getIp() {
        return ip;
    }

    public InetAddress getIpInet() { return ipInet; }

    public int getPort() {
        return port;
    }

    @Override
    public int compareTo(Object o) {
        return this.getId() - ((Host)o).getId();
    }
}
