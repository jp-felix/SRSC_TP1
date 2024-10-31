package main.java.examples.streaming;/*
 * hjStreamServer.java
 * Streaming server: emitter of video streams (movies)
 * Can send in unicast or multicast IP for client listeners
 * that can play in real time the transmitted movies
 */

import main.java.dstp.DSTPSocket;

import java.io.*;
import java.net.*;

class hjStreamServer {

    static public void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("Usage: hjStreamServer <movie> <ip-address> <port>");
            System.exit(-1);
        }

        DataInputStream movieStream = new DataInputStream(new FileInputStream(args[0]));
        InetSocketAddress address = new InetSocketAddress(args[1], Integer.parseInt(args[2]));

        String configFilePath = "config/cryptoconfig.txt";
        DSTPSocket dstpSocket = new DSTPSocket(configFilePath, false); // Server mode

        byte[] buffer = new byte[65000];
        long referenceTime = System.nanoTime();
        long initialFrameTime = 0;

        int packetCount = 0;
        while (movieStream.available() > 0) {
            int frameSize = movieStream.readShort();
            long frameTimestamp = movieStream.readLong();

            if (packetCount == 0) initialFrameTime = frameTimestamp;

            movieStream.readFully(buffer, 0, frameSize);
            long currentTime = System.nanoTime();
            Thread.sleep(Math.max(0, ((frameTimestamp - initialFrameTime) - (currentTime - referenceTime)) / 1_000_000));

            // Send the encrypted frame data with DSTPSocket
            dstpSocket.send(buffer, 0, frameSize, address);
            packetCount++;
        }

        System.out.println("Streaming finished. Packets sent: " + packetCount);

        movieStream.close();
        dstpSocket.close();
    }
}
