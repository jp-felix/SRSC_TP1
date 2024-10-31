package test.java.dstp;

import main.java.dstp.DSTPSocket;

import java.net.InetAddress;

public class DSTPSocketTest {
    public static void main(String[] args) {
        try {
            String configFilePath = "config/cryptoconfig.txt";

            // Receiver socket using port 5000
            DSTPSocket receiverSocket = new DSTPSocket(5000, configFilePath, false);

            // Sender socket using port 5001
            DSTPSocket senderSocket = new DSTPSocket(5001, configFilePath, false);


            String message = "TEST!!!!";
            System.out.println("Message sent: " + message);

            // Sender sends the message
            senderSocket.send(message.getBytes(), InetAddress.getLocalHost(), 5000);
            System.out.println("Message sent.");

            // Receiver receives and decrypts the message
            byte[] receivedData = receiverSocket.receive();
            String receivedMessage = new String(receivedData);
            System.out.println("Message received: " + receivedMessage);

            if(message.equals(receivedMessage)){
                System.out.println("MATCH");
            }else{
                System.out.println("MESSAGES DONT MATCH");
            }

            senderSocket.close();
            receiverSocket.close();

        } catch (Exception e) {
            System.err.println("Error during DSTPSocket test: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
//Test Result: (SUCCESS!!!!!)
/*
Message sent: TEST!!!!
Message sent.
Message received: TEST!!!!
MATCH
* */
