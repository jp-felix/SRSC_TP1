package main.java.examples.tftp;

import java.net.*;
import java.io.*;

import main.java.dstp.DSTPSocket;


class TFTPclientWRQ {
    protected InetAddress server;
    protected String fileName;
    protected String dataMode;
    private DSTPSocket dstpSocket;

    public TFTPclientWRQ(InetAddress ip, String name, String mode, DSTPSocket dstpSocket) throws Exception {
        this.server = ip;
        this.fileName = name;
        this.dataMode = mode;
        this.dstpSocket = dstpSocket;

        try {
            dstpSocket.setSoTimeout(2000); // Set a 2-second timeout
            int timeoutLimit = 5; // Retry limit for timeouts

            // Start timing
            long startTime = System.currentTimeMillis();

            FileInputStream source = new FileInputStream("../files/" + fileName);

            // Send a secure write request to the server
            TFTPwrite reqPak = new TFTPwrite(fileName, dataMode);
            reqPak.send(server, 6973, dstpSocket);

            // Wait for the acknowledgment or error packet from the server
            TFTPpacket sendRsp = TFTPpacket.receive(dstpSocket);
            int port = sendRsp.getPort(); // new port for transfer
            /*System.out.println("new port " + sendRsp.getPort());*/

            //check packet type
            if (sendRsp instanceof TFTPack) {
                TFTPack Rsp = (TFTPack) sendRsp;
                System.out.println("--Server ready--\nUploading");
            } else if (sendRsp instanceof TFTPerror) {
                TFTPerror Rsp = (TFTPerror) sendRsp;
                source.close();
                throw new TftpException(Rsp.message());
            }

            int bytesRead = TFTPpacket.maxTftpPakLen;

            // Begin the file upload process
            for (int blkNum = 1; bytesRead == TFTPpacket.maxTftpPakLen; blkNum++) {
                // Create a data packet with the next block of data
                TFTPdata outPak = new TFTPdata(blkNum, source);
                bytesRead = outPak.getLength();
                /*System.out.println("block no. " + outPak.blockNumber());*/

                outPak.send(server, port, dstpSocket);

                //visual effect to user
                if (blkNum % 500 == 0) {
                    System.out.print("\b.>");
                }
                if (blkNum % 15000 == 0) {
                    System.out.println("\b.");
                }

                while (timeoutLimit != 0) { // wait for the correct ack
                    try {
                        TFTPpacket ack = TFTPpacket.receive(dstpSocket);
                        if (!(ack instanceof TFTPack))
                            break;

                        TFTPack a = (TFTPack) ack;

                        // Check if the response is from the correct port
                        if (port != a.getPort()) continue;
                        /*System.out.println("got response from server");*/

                        // Resend packet if an acknowledgment for a different block is received
                        if (a.blockNumber() != blkNum) {
                            System.out.println("Last packet lost, resend packet");
                            throw new SocketTimeoutException("Last packet lost, resend packet");
                        }

                        /*System.out.println("response blk no. " + a.blockNumber());*/

                        break;
                    } catch (SocketTimeoutException t0) {
                        System.out.println("Resend blk " + blkNum);
                        outPak.send(server, port, dstpSocket); // resend the last packet
                        timeoutLimit--;
                    }
                }
                if (timeoutLimit == 0) {
                    throw new TftpException("connection failed");
                }

            }

            // End timing
            long endTime = System.currentTimeMillis();
            long transferTime = endTime - startTime;

            System.out.println("\nUpload finished!\nFilename: " + fileName);
            System.out.println("Transfer time: " + transferTime + " ms");
            System.out.println("SHA-256 Checksum: " + CheckSum.getChecksum("../files/" + fileName));

            // Log transfer details
            try (PrintWriter logWriter = new PrintWriter(new FileWriter("transfer_log.txt", true))) {
                logWriter.println("Upload started: [" + fileName + "] to " + server.getHostAddress() + ":" + 6973);
                logWriter.println("Transfer time: " + transferTime + " ms");
                logWriter.println("Transfer status: Success\n");
            } catch (IOException e) {
                System.out.println("Failed to write log: " + e.getMessage());
            }

            source.close();
            dstpSocket.close();
        } catch (SocketTimeoutException t) {
            System.out.println("No response from sever, please try again");
        } catch (IOException e) {
            System.out.println("IO error, transfer aborted");
            try (PrintWriter logWriter = new PrintWriter(new FileWriter("transfer_log.txt", true))) {
                logWriter.println("Upload failed for file: [" + fileName + "] to " + server.getHostAddress() + ":" + 6973);
                logWriter.println("Reason: IO error, transfer aborted\n");
            }
        } catch (TftpException e) {
            System.out.println(e.getMessage());
            try (PrintWriter logWriter = new PrintWriter(new FileWriter("transfer_log.txt", true))) {
                logWriter.println("Upload failed for file: [" + fileName + "] to " + server.getHostAddress() + ":" + 6973);
                logWriter.println("Reason: " + e.getMessage() + "\n");
            }
        }
    }
}
