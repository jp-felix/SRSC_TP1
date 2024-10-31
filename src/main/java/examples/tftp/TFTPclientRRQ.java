package main.java.examples.tftp;

import java.io.*;
import java.net.*;

import main.java.dstp.DSTPSocket;

class TFTPclientRRQ {
    protected InetAddress server;
    protected String fileName;
    protected String dataMode;
    private DSTPSocket dstpSocket;

    public TFTPclientRRQ(InetAddress ip, String name, String mode, DSTPSocket dstpSocket) throws Exception {
        this.server = ip;
        this.fileName = name;
        this.dataMode = mode;
        this.dstpSocket = dstpSocket;

        try {
            dstpSocket.setSoTimeout(2000); // set time out to 2s

            // Start timing
            long startTime = System.currentTimeMillis();

            FileOutputStream outFile = new FileOutputStream("../files/" + fileName);

            // Send a secure read request packet
            TFTPread reqPak = new TFTPread(fileName, dataMode);
            reqPak.send(server, 6973, dstpSocket);

            TFTPack ack = null;
            InetAddress newIP = server; // for transfer
            int newPort = 0; // for transfer
            int timeoutLimit = 5; // Retry limit
            int testloss = 1; // test only

            // Process the transfer
            System.out.println("Downloading");
            for (int blkNum = 1, bytesOut = 512; bytesOut == 512; blkNum++) {
                while (timeoutLimit != 0) {
                    try {
                        // Securely receive a packet
                        TFTPpacket inPak = TFTPpacket.receive(dstpSocket);

                        //check packet type
                        if (inPak instanceof TFTPerror) {
                            TFTPerror p = (TFTPerror) inPak;
                            throw new TftpException(p.message());
                        } else if (inPak instanceof TFTPdata) {
                            TFTPdata p = (TFTPdata) inPak;

                            // Visual feedback for large downloads
                            if (blkNum % 500 == 0) System.out.print("\b.>");
                            if (blkNum % 15000 == 0) System.out.println("\b.");

                            newIP = p.getAddress();

                            // Check if port matches previous packet's port
                            if (newPort != 0 && newPort != p.getPort()) continue; // Ignore packet if port doesn't match
                            newPort = p.getPort();

                            // Verify block number
                            if (blkNum != p.blockNumber()) throw new SocketTimeoutException();

                            // everything is fine then write to the file
                            bytesOut = p.write(outFile);

                            // Send acknowledgment for received block
                            ack = new TFTPack(blkNum);
                            ack.send(newIP, newPort, dstpSocket);
                            break;
                        } else
                            throw new TftpException("Unexpected response from server");
                    } catch (SocketTimeoutException t) {
                        // Handle timeout during read request or acknowledgment
                        if (blkNum == 1) {
                            System.out.println("Failed to reach the server. Retrying...");
                            reqPak.send(server, 6973, dstpSocket);
                        } else {
                            System.out.println("Connection timeout, resending last ack. Timeout limit left=" + timeoutLimit);
                            ack = new TFTPack(blkNum - 1);
                            ack.send(newIP, newPort, dstpSocket);
                        }
                        timeoutLimit--;
                    }
                }
                if (timeoutLimit == 0) {
                    throw new TftpException("Connection failed");
                }
            }
            // End timing
            long endTime = System.currentTimeMillis();
            long transferTime = endTime - startTime;

            System.out.println("\nDownload Finished.\nFilename: " + fileName);
            System.out.println("Transfer time: " + transferTime + " ms");
            System.out.println("SHA-256 Checksum: " + CheckSum.getChecksum("../files/" + fileName));

            // Log transfer details
            try (PrintWriter logWriter = new PrintWriter(new FileWriter("transfer_log.txt", true))) {
                logWriter.println("Transfer started: [" + fileName + "] from " + server.getHostAddress() + ":" + 6973);
                logWriter.println("Transfer time: " + transferTime + " ms");
                logWriter.println("Transfer status: Success\n");
            } catch (IOException e) {
                System.out.println("Failed to write log: " + e.getMessage());
            }

            outFile.close();
            dstpSocket.close();
        } catch (IOException e) {
            System.out.println("IO error, transfer aborted");
            try (PrintWriter logWriter = new PrintWriter(new FileWriter("transfer_log.txt", true))) {
                logWriter.println("Transfer failed for file: [" + fileName + "] from " + server.getHostAddress() + ":" + 6973);
                logWriter.println("Reason: IO error, transfer aborted\n");
            }
            File wrongFile = new File(fileName);
            wrongFile.delete();
        } catch (TftpException e) {
            System.out.println(e.getMessage());
            try (PrintWriter logWriter = new PrintWriter(new FileWriter("transfer_log.txt", true))) {
                logWriter.println("Transfer failed for file: [" + fileName + "] from " + server.getHostAddress() + ":" + 6973);
                logWriter.println("Reason: " + e.getMessage() + "\n");
            }
            File wrongFile = new File(fileName);
            wrongFile.delete();
        }
    }
}
