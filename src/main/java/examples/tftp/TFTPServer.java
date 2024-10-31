package main.java.examples.tftp;

import java.net.*;
import java.io.*;
import java.util.*;

import main.java.dstp.DSTPSocket;

public class TFTPServer {

    public static void main(String argv[]) {
        DSTPSocket dstpSocket = null;

        try {
            String configFilePath = "config/cryptoconfig.txt";
            dstpSocket = new DSTPSocket(6973, configFilePath, true);
            dstpSocket.setSoTimeout(2000); // Set timeout for 2 seconds

            System.out.println("Server Ready.  Port:  " + dstpSocket.getLocalPort());

            // Listen for requests
            while (true) {
                try {
                    TFTPpacket in = TFTPpacket.receive(dstpSocket);

                    // Process read request
                    if (in instanceof TFTPread) {
                        System.out.println("Read Request from " + in.getAddress());
                        TFTPserverRRQ r = new TFTPserverRRQ((TFTPread) in, dstpSocket);
                    }
                    // Process write request
                    else if (in instanceof TFTPwrite) {
                        System.out.println("Write Request from " + in.getAddress());
                        TFTPserverWRQ w = new TFTPserverWRQ((TFTPwrite) in, dstpSocket);
                    }
                } catch (SocketTimeoutException e) {
                    // Handle timeout without terminating the server, its was terminating the server immediately
                    System.out.println("Waiting for new requests...");
                } catch (TftpException e) {
                    System.out.println("Server encountered a TFTP error: " + e.getMessage());
                } catch (IOException e) {
                    System.out.println("Server encountered an IOException: " + e.getMessage());
                    break; // Only break the loop for non-timeout IOExceptions
                }
            }
        } catch (SocketException e) {
            System.out.println("Server terminated(SocketException) " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Server terminated(IOException)" + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (dstpSocket != null)
                dstpSocket.close();
        }
    }
}