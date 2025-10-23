package ru.nsu.nocode;

import ru.nsu.nocode.server.KeyServer;
import ru.nsu.nocode.server.CertificateUtils;
import ru.nsu.nocode.client.KeyClient;

import java.nio.file.Path;
import java.security.PrivateKey;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("""
                    Usage:
                      java ru.nsu.nocode.Main server <port> <threads> <issuer.pem> <issuerDN>
                      java ru.nsu.nocode.Main client <host> <port> <name> [--delay N] [--exit-before-read]
                    """);
            System.exit(1);
        }

        try {
            switch (args[0].toLowerCase()) {
                case "server" -> runServer(args);
                case "client" -> runClient(args);
                default -> {
                    System.err.println("Unknown mode: " + args[0]);
                    System.exit(2);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runServer(String[] args) throws Exception {
        if (args.length < 5) {
            System.err.println("Usage: server <port> <threads> <issuer.pem> <issuerDN>");
            return;
        }
        int port = Integer.parseInt(args[1]);
        int threads = Integer.parseInt(args[2]);
        Path pem = Path.of(args[3]);
        String dn = args[4];
        PrivateKey issuerKey = CertificateUtils.loadPrivateKeyPem(pem);
        KeyServer server = new KeyServer(port, threads, issuerKey, dn);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Stopping server...");
            server.shutdown();
        }));

        server.start();
    }

    private static void runClient(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("Usage: client <host> <port> <name> [--delay N] [--exit-before-read]");
            return;
        }

        String[] clientArgs = new String[args.length - 1];
        System.arraycopy(args, 1, clientArgs, 0, clientArgs.length);
        KeyClient.main(clientArgs);
    }
}
