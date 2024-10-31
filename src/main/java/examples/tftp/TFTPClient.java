package main.java.examples.tftp;

import main.java.dstp.DSTPSocket;

import java.net.InetAddress;
import java.net.UnknownHostException;

class UseException extends Exception {
    public UseException() {
        super();
    }

    public UseException(String s) {
        super(s);
    }
}

public class TFTPClient {
    public static void main(String argv[]) throws UseException {
        String host = "";
        String fileName = "";
        String mode = "octet"; //default mode
        String type = "";
        DSTPSocket dstpSocket = null; // DSTPSocket for secure communication

        try {
            // Process command line
            if (argv.length == 0)
                throw new UseException("--Usage-- \nocter mode:  TFTPClient [host] [Type(R/W?)] [filename] \nother mode:  TFTPClient [host] [Type(R/W?)] [filename] [mode]");

            //use default mode(octet)
            if (argv.length == 3) {
                host = argv[0];
                type = argv[argv.length - 2];
                fileName = argv[argv.length - 1];
            }
            //use other modes
            else if (argv.length == 4) {
                host = argv[0];
                mode = argv[argv.length - 1];
                type = argv[argv.length - 3];
                fileName = argv[argv.length - 2];
            } else
                throw new UseException("wrong command. \n--Usage-- \nocter mode:  TFTPClient [host] [Type(R/W?)] [filename] \nother mode:  TFTPClient [host] [Type(R/W?)] [filename] [mode]");


            InetAddress server = InetAddress.getByName(host);
            String configFilePath = "config/cryptoconfig.txt";
            int port = 6973; // Default port for TFTP

            dstpSocket = new DSTPSocket(port, configFilePath, true);

            // Initialize DSTPSocket without specifying a port, so the OS assigns a free port
            //dstpSocket = new DSTPSocket(configFilePath, false);


            // Process read request
            if (type.matches("R")) {
                TFTPclientRRQ r = new TFTPclientRRQ(server, fileName, mode, dstpSocket);
            }
            // Process write request
            else if (type.matches("W")) {
                TFTPclientWRQ w = new TFTPclientWRQ(server, fileName, mode, dstpSocket);
            } else {
                throw new UseException("wrong command. \n--Usage-- \nocter mode:  TFTPClient [host] [Type(R/W?)] [filename] \nother mode:  TFTPClient [host] [Type(R/W?)] [filename] [mode]");
            }

        } catch (UnknownHostException e) {
            System.out.println("Unknown host " + host);
        } catch (UseException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (dstpSocket != null) {
                dstpSocket.close();
            }
        }
    }
}