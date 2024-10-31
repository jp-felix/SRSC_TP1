package main.java.dstp;

import main.java.dstp.config.CryptoConfigLoader;

import java.io.IOException;
import java.net.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class DSTPSocket {
    private MulticastSocket multicastSocket;  // Use MulticastSocket for multicast compatibility
    private DatagramSocket datagramSocket;
    private SecretKey encryptionKey;
    private SecretKey macKey;
    private Cipher encryptionCipher;
    private Mac hmac;

    // Constructor for multicast
    public DSTPSocket(int port, String configFilePath, boolean isMulticast) throws Exception {
        if (isMulticast) {
            multicastSocket = new MulticastSocket(port);  // Initialize MulticastSocket
            System.out.println("MulticastSocket initialized on port: " + port);
        } else {
            datagramSocket = new DatagramSocket(port);    // Initialize DatagramSocket for unicast
            System.out.println("DatagramSocket initialized on port: " + port);
        }

        // Load cryptographic configurations
        CryptoConfigLoader configLoader = new CryptoConfigLoader(configFilePath);
        setupKeysAndCiphers(configLoader);
    }

    // Constructor for client mode without a specific port
    public DSTPSocket(String configFilePath, boolean isClient) throws Exception {
        this.datagramSocket = new DatagramSocket(); // Automatically assign a port for client
        CryptoConfigLoader configLoader = new CryptoConfigLoader(configFilePath);
        setupKeysAndCiphers(configLoader);
    }

    private void setupKeysAndCiphers(CryptoConfigLoader configLoader) throws Exception {
        // Load encryption key
        byte[] encKeyBytes = hexToBytes(configLoader.getSymmetricKey());
        encryptionKey = new SecretKeySpec(encKeyBytes, configLoader.getConfidentialityAlgorithm().split("/")[0]);

        // Initialize encryption cipher with IV
        encryptionCipher = Cipher.getInstance(configLoader.getConfidentialityAlgorithm());
        IvParameterSpec iv = new IvParameterSpec(hexToBytes(configLoader.getIv()));
        encryptionCipher.init(Cipher.ENCRYPT_MODE, encryptionKey, iv);

        // Initialize HMAC
        byte[] macKeyBytes = hexToBytes(configLoader.getMacKey());
        macKey = new SecretKeySpec(macKeyBytes, "Hmac" + configLoader.getHashAlgorithm());
        hmac = Mac.getInstance("Hmac" + configLoader.getHashAlgorithm());
        hmac.init(macKey);
    }

    // Join a multicast group (for receivers)
    public void joinGroup(InetAddress group) throws IOException {
        if (multicastSocket != null) {
            multicastSocket.joinGroup(group);
        } else {
            throw new IllegalStateException("MulticastSocket is not initialized for multicast operations.");
        }
    }

    // Leave a multicast group
    public void leaveGroup(InetAddress group) throws IOException {
        if (multicastSocket != null) {
            multicastSocket.leaveGroup(group);
        } else {
            throw new IllegalStateException("MulticastSocket is not initialized for multicast operations.");
        }
    }

    // Send encrypted data
    public void send(byte[] data, InetAddress address, int port) throws Exception {
        byte[] encryptedData = encryptionCipher.doFinal(data);
        byte[] mac = hmac.doFinal(encryptedData);
        byte[] packetData = new byte[encryptedData.length + mac.length];
        System.arraycopy(encryptedData, 0, packetData, 0, encryptedData.length);
        System.arraycopy(mac, 0, packetData, encryptedData.length, mac.length);

        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, address, port);

        if (multicastSocket != null) {
            multicastSocket.send(packet);
        } else if (datagramSocket != null) {
            datagramSocket.send(packet);
        } else {
            throw new IllegalStateException("No socket is initialized for sending.");
        }
    }

    public void send(byte[] data, int offset, int length, InetSocketAddress address) throws Exception {
        byte[] dataToEncrypt = new byte[length];
        System.arraycopy(data, offset, dataToEncrypt, 0, length);
        byte[] encryptedData = encryptionCipher.doFinal(dataToEncrypt);

        byte[] mac = hmac.doFinal(encryptedData);
        byte[] packetData = new byte[encryptedData.length + mac.length];
        System.arraycopy(encryptedData, 0, packetData, 0, encryptedData.length);
        System.arraycopy(mac, 0, packetData, encryptedData.length, mac.length);

        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, address);

        if (multicastSocket != null) {
            multicastSocket.send(packet);
        } else if (datagramSocket != null) {
            datagramSocket.send(packet);
        } else {
            throw new IllegalStateException("No socket is initialized for sending.");
        }
    }

    public void send(byte[] data, InetSocketAddress address) throws Exception {
        byte[] encryptedData = encryptionCipher.doFinal(data);
        byte[] mac = hmac.doFinal(encryptedData);
        byte[] packetData = new byte[encryptedData.length + mac.length];
        System.arraycopy(encryptedData, 0, packetData, 0, encryptedData.length);
        System.arraycopy(mac, 0, packetData, encryptedData.length, mac.length);

        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, address);

        if (multicastSocket != null) {
            multicastSocket.send(packet);
        } else if (datagramSocket != null) {
            datagramSocket.send(packet);
        } else {
            throw new IllegalStateException("No socket is initialized for sending.");
        }
    }

    // Receive data with integrity check
    public byte[] receive() throws Exception {
        byte[] buffer = new byte[65535];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        if (multicastSocket != null) {
            multicastSocket.receive(packet);
        } else if (datagramSocket != null) {
            datagramSocket.receive(packet);
        } else {
            throw new IllegalStateException("No socket is initialized for receiving.");
        }

        int dataLength = packet.getLength() - hmac.getMacLength();
        byte[] encryptedData = new byte[dataLength];
        byte[] receivedMac = new byte[hmac.getMacLength()];
        System.arraycopy(buffer, 0, encryptedData, 0, dataLength);
        System.arraycopy(buffer, dataLength, receivedMac, 0, receivedMac.length);

        byte[] calculatedMac = hmac.doFinal(encryptedData);
        if (!MessageDigest.isEqual(receivedMac, calculatedMac)) {
            throw new SecurityException("Integrity check failed.");
        }

        encryptionCipher.init(Cipher.DECRYPT_MODE, encryptionKey, encryptionCipher.getParameters());
        return encryptionCipher.doFinal(encryptedData);
    }

    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    public void setSoTimeout(int timeout) throws SocketException {
        if (datagramSocket != null) {
            datagramSocket.setSoTimeout(timeout);
        } else if (multicastSocket != null) {
            multicastSocket.setSoTimeout(timeout);
        }
    }

    public int getLocalPort() {
        if (datagramSocket != null) {
            return datagramSocket.getLocalPort();
        } else if (multicastSocket != null) {
            return multicastSocket.getLocalPort();
        }
        return -1;
    }

    public void close() {
        if (multicastSocket != null) {
            multicastSocket.close();
        }
        if (datagramSocket != null) {
            datagramSocket.close();
        }
    }
}
