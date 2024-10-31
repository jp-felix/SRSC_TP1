package main.java.examples.streaming;

import main.java.dstp.DSTPSocket;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class hjUDPproxy {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Use: hjUDPproxy <endpoint1> <endpoint2>");
            System.out.println("<endpoint1>: endpoint for receiving stream");
            System.out.println("<endpoint2>: endpoint(s) of media player (comma-separated for multiple destinations)");
            System.exit(0);
        }

        String remote = args[0]; // Endpoint for receiving stream
        String destinations = args[1]; // Destination endpoint(s) for forwarding

        SocketAddress inSocketAddress = parseSocketAddress(remote);
        Set<SocketAddress> outSocketAddressSet = Arrays.stream(destinations.split(","))
                .map(hjUDPproxy::parseSocketAddress)
                .collect(Collectors.toSet());

        // Initialize DSTPSocket for secure receiving and forwarding
        String configFilePath = "config/cryptoconfig.txt";
        DSTPSocket inSocket = new DSTPSocket(configFilePath, false); // Server mode to receive
        DSTPSocket outSocket = new DSTPSocket(configFilePath, true); // Client mode for sending

        byte[] buffer = new byte[65535];
        while (true) {
            // Receive encrypted packet from the source endpoint
            byte[] receivedData = inSocket.receive();

            // Forward the decrypted data securely to each destination
            for (SocketAddress outSocketAddress : outSocketAddressSet) {
                InetSocketAddress inetAddress = (InetSocketAddress) outSocketAddress;
                outSocket.send(receivedData, inetAddress); // Securely forward to destination
            }
        }
    }

    private static InetSocketAddress parseSocketAddress(String socketAddress) {
        String[] split = socketAddress.split(":");
        String host = split[0];
        int port = Integer.parseInt(split[1]);
        return new InetSocketAddress(host, port);
    }
}
