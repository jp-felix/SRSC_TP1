package main.java.examples.tftp;

import java.io.*;
import java.net.*;

import main.java.dstp.DSTPSocket;


class TFTPserverRRQ extends Thread {

    protected DSTPSocket dstpSocket;
    protected InetAddress host;
    protected FileInputStream source;
    protected TFTPpacket req;
    protected String fileName;
    protected int port;
    protected int timeoutLimit = 5;

    // initialize read request
    public TFTPserverRRQ(TFTPread request, DSTPSocket dstpSocket) throws TftpException {
        try {
            this.dstpSocket = dstpSocket;
            this.req = request;
            dstpSocket.setSoTimeout(1000);

            fileName = request.fileName();
            host = request.getAddress();
            port = request.getPort();

            //create file object in parent folder
            File srcFile = new File("../files/" + fileName);

            //Check if the file exists and is readable
            if (srcFile.exists() && srcFile.isFile() && srcFile.canRead()) {
                source = new FileInputStream(srcFile);
                this.start(); //open new thread for transfer
            } else {
                throw new TftpException("access violation");
            }
        } catch (Exception e) {
            TFTPerror ePak = new TFTPerror(1, e.getMessage()); // error code 1
            try {
                ePak.send(host, port, dstpSocket);
            } catch (Exception f) {
                System.out.println("Failed to send error packet: " + f.getMessage());
            }
            System.out.println("Client start failed:  " + e.getMessage());
        }
    }

    //everything is fine, open new thread to transfer file
    public void run() {
        int bytesRead = TFTPpacket.maxTftpPakLen;

        // Handle read request
        if (req instanceof TFTPread) {
            try {
                for (int blkNum = 1; bytesRead == TFTPpacket.maxTftpPakLen; blkNum++) {
                    TFTPdata outPak = new TFTPdata(blkNum, source);
                    bytesRead = outPak.getLength();
                    /*System.out.println("send block no. " + outPak.blockNumber()); */
                    /*System.out.println("bytes sent:  " + bytesRead);*/

                    outPak.send(host, port, dstpSocket);
                    /*System.out.println("current op code  " + outPak.get(0)); */

                    //Wait for the correct acknowledgment; retry up to 5 times if incorrect
                    while (timeoutLimit != 0) {
                        try {
                            TFTPpacket ack = TFTPpacket.receive(dstpSocket);
                            if (!(ack instanceof TFTPack)) {
                                throw new Exception("Client failed");
                            }
                            TFTPack a = (TFTPack) ack;

                            // Verify acknowledgment block number
                            if (a.blockNumber() != blkNum) { //check ack
                                throw new SocketTimeoutException("last packet lost, resend packet");
                            }
                            break;
                        } catch (SocketTimeoutException t) {
                            // Resend the last packet if acknowledgment is not received
                            System.out.println("Resent blk " + blkNum);
                            timeoutLimit--;
                            outPak.send(host, port, dstpSocket);
                        }
                    }
                    if (timeoutLimit == 0) {
                        throw new Exception("connection failed");
                    }
                }
                // Transfer complete
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
            }
        }
    }
}
