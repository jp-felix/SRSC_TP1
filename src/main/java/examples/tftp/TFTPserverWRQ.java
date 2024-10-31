package main.java.examples.tftp;

import java.net.*;
import java.io.*;

import main.java.dstp.DSTPSocket;

class TFTPserverWRQ extends Thread {

    protected DSTPSocket dstpSocket;
    protected InetAddress host;
    protected FileOutputStream outFile;
    protected TFTPpacket req;
    protected File saveFile;
    protected String fileName;
    protected int timeoutLimit = 5;
    protected int port;
    //protected int testloss=0;

    // Initialize write request
    public TFTPserverWRQ(TFTPwrite request, DSTPSocket dstpSocket) throws TftpException {
        try {
            req = request;
            this.dstpSocket = dstpSocket;
            dstpSocket.setSoTimeout(1000); // Set a 1-second timeout

            host = request.getAddress();
            port = request.getPort();
            fileName = request.fileName();

            //create file object in parent folder
            saveFile = new File("../files/" + fileName);

            // Check if the file already exists
            if (!saveFile.exists()) {
                outFile = new FileOutputStream(saveFile);

                TFTPack a = new TFTPack(0);
                a.send(host, port, dstpSocket); // send ack 0 at first, ready to receive

                this.start(); // Start the file reception in a new thread
            } else
                throw new TftpException("access violation, file exists");
        } catch (Exception e) {
            TFTPerror ePak = new TFTPerror(1, e.getMessage()); // error code 1
            try {
                ePak.send(host, port, dstpSocket);
            } catch (Exception f) {
                System.out.println("Failed to send error packet: " + f.getMessage());
            }
            System.out.println("Client start failed:" + e.getMessage());
        }
    }

    public void run() {
        // handle write request
        if (req instanceof TFTPwrite) {
            try {
                for (int blkNum = 1, bytesOut = 512; bytesOut == 512; blkNum++) {
                    while (timeoutLimit != 0) {
                        try {
                            // Receive the incoming data packet securely
                            TFTPpacket inPak = TFTPpacket.receive(dstpSocket);

                            //check packet type
                            if (inPak instanceof TFTPerror) {
                                TFTPerror p = (TFTPerror) inPak;
                                throw new TftpException(p.message());
                            } else if (inPak instanceof TFTPdata) {
                                TFTPdata p = (TFTPdata) inPak;

                                // Verify the block number is as expected
                                if (p.blockNumber() != blkNum) {
                                    throw new SocketTimeoutException("Block mismatch, expected block " + blkNum);
                                }

                                //Write received data to the file
                                bytesOut = p.write(outFile);

                                // Send acknowledgment for the received block
                                TFTPack a = new TFTPack(blkNum);
                                a.send(host, port, dstpSocket);
                                break;
                            }
                        } catch (SocketTimeoutException t2) {
                            // Handle timeout by resending the last acknowledgment
                            System.out.println("Timeout, resending acknowledgment for block " + (blkNum - 1));
                            TFTPack a = new TFTPack(blkNum - 1);
                            a.send(host, port, dstpSocket);
                            timeoutLimit--;
                        }
                    }
                    if (timeoutLimit == 0) {
                        throw new Exception("Connection failed");
                    }
                }
                System.out.println("Transfer completed.(Client " + host + ")");
                System.out.println("Filename: " + fileName + "\nSHA-256 checksum: " + CheckSum.getChecksum("../files/" + fileName) + "\n");

            } catch (Exception e) {
                TFTPerror ePak = new TFTPerror(1, e.getMessage());
                try {
                    ePak.send(host, port, dstpSocket);
                } catch (Exception f) {
                    System.out.println("Failed to send error packet: " + f.getMessage());
                }
                System.out.println("Client failed:  " + e.getMessage());
                saveFile.delete();
            }
        }
    }
}
