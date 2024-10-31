package examples.multicast;

import main.java.dstp.DSTPSocket;
import java.net.InetAddress;

public class MulticastReceiver {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("usage: java MulticastReceiver grupo_multicast porto");
            System.exit(0);
        }

        int port = Integer.parseInt(args[1]);
        InetAddress group = InetAddress.getByName(args[0]);
        String configFilePath = "config/cryptoconfig.txt";

        // Ensure multicast address is provided
        if (!group.isMulticastAddress()) {
            System.err.println("Multicast address required...");
            System.exit(0);
        }

        // Initialize DSTPSocket with isMulticast = true
        DSTPSocket dstpSocket = new DSTPSocket(port, configFilePath, true); // Ensure true for multicast
        System.out.println("DSTPSocket created with multicast support.");

        // Join the group
        dstpSocket.joinGroup(group);  // This should use multicastSocket

        String recvmsg;
        do {
            byte[] receivedData = dstpSocket.receive();
            recvmsg = new String(receivedData);
            System.out.println("Received message: " + recvmsg);
        } while (!"fim!".equals(recvmsg));

        dstpSocket.close();
    }
}

