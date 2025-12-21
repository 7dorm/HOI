package ru.nsu.nocode.client;

import java.io.*;
import java.lang.reflect.Array;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class KeyClient {
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Usage: KeyClient <host> <port> <name> [--delay N] [--preexit]");
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String name = args[2];
        int delay = 0; boolean exitEarly = false;

        for (int i = 3; i < args.length; i++) {
            if ("--delay".equals(args[i]) && i + 1 < args.length)
                delay = Integer.parseInt(args[++i]);
            else if ("--preexit".equals(args[i]))
                exitEarly = true;
        }

        try (Socket sock = new Socket(host, port)) {
            OutputStream os = sock.getOutputStream();
            InputStream is = sock.getInputStream();

            byte[] nameBytes = name.getBytes(StandardCharsets.US_ASCII);
            os.write(nameBytes);
            os.write(0);
            os.flush();
            System.out.println("Sent: " + name);

            if (exitEarly) {
                System.out.println("Exiting early.");
                return;
            }
            if (delay > 0) {
                System.out.println("Delaying " + delay + "s...");
                Thread.sleep(delay * 1000L);
            }


            byte[] statusBytes = is.readNBytes(1);
            if (statusBytes.length < 1) throw new IOException("Incomplete status");
            byte status = statusBytes[0];

            if (status == 1) {
                byte[] lenBytes = is.readNBytes(4);
                if (lenBytes.length < 4) throw new IOException("Incomplete error header");
                int errorLen = ByteBuffer.wrap(lenBytes).getInt();
                byte[] errorBytes = is.readNBytes(errorLen);
                String errorMsg = new String(errorBytes, StandardCharsets.UTF_8);
                System.err.println("Server error: " + errorMsg);
                return;
            } else if (status == 0) {
                byte[] buf4 = is.readNBytes(4);
                if (buf4.length < 4) throw new IOException("Incomplete header");
                int privLen = ByteBuffer.wrap(buf4).getInt();
                System.out.println("Received private key length: " + privLen);
                byte[] priv = is.readNBytes(privLen);
                System.out.println("Received private key bytes: " + priv.length);
                byte[] buf4b = is.readNBytes(4);
                int certLen = ByteBuffer.wrap(buf4b).getInt();
                System.out.println("Received certificate length: " + certLen);
                byte[] cert = is.readNBytes(certLen);
                System.out.println("Received certificate bytes: " + cert.length);

                try (FileOutputStream f = new FileOutputStream(name + ".key")) { f.write(priv); }
                try (FileOutputStream f = new FileOutputStream(name + ".crt")) { f.write(cert); }
                System.out.println("Saved " + name + ".key and " + name + ".crt");
            } else {
                System.err.println("Unknown status: " + status);
            }
        }
    }
}
