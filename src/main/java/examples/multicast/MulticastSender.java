package examples.multicast;

import main.java.dstp.DSTPSocket;
import java.net.InetAddress;
import java.util.Date;

public class MulticastSender {

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("usage: java MulticastSender grupo_multicast porto time-interval");
            System.exit(0);
        }

        // Load arguments
        int port = Integer.parseInt(args[1]);
        InetAddress group = InetAddress.getByName(args[0]);
        int timeInterval = Integer.parseInt(args[2]);
        int more = 20; // Number of times to send the message
        String configFilePath = "config/cryptoconfig.txt"; // Path to crypto configuration

        // Ensure a multicast address is provided
        if (!group.isMulticastAddress()) {
            System.err.println("Multicast address required...");
            System.exit(0);
        }

        // Initialize DSTPSocket with multicast compatibility
        DSTPSocket dstpSocket = new DSTPSocket(port, configFilePath, true);

        // Loop to send multiple messages
        do {
            // Prepare the secure message with timestamp
            String msgSecret = "Top secret message, sent on: ";
            String msgDate = new Date().toString();
            String msg = msgSecret + msgDate;

            // Send the encrypted and authenticated message using DSTPSocket
            dstpSocket.send(msg.getBytes(), group, port);
            System.out.println("Sent secure message: " + msg);

            // Decrease counter
            --more;

            // Wait before sending the next message
            try {
                Thread.sleep(1000 * timeInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        } while (more > 0);

        // Send a final message to indicate completion
        String finalMessage = "fim!";
        dstpSocket.send(finalMessage.getBytes(), group, port);
        System.out.println("Sent final message: " + finalMessage);

        // Close the DSTPSocket
        dstpSocket.close();
    }
}
