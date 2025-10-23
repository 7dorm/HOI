package ru.nsu.nocode.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class KeyServer {
    private final int port;
    private final int genThreads;
    private final PrivateKey issuerKey;
    private final String issuerDN;
    private final Selector selector;
    private final ServerSocketChannel serverChannel;
    private final ExecutorService pool;
    private final ConcurrentHashMap<String, CompletableFuture<PairPem>> cache = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<ClientConnection> readyToWrite = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean running = new AtomicBoolean(true);

    public void shutdown() {
        System.out.println("Shutting down KeyServer...");

        running.set(false);

        if (selector != null && selector.isOpen()) {
            selector.wakeup();
        }

        try {
            if (serverChannel != null && serverChannel.isOpen()) {
                serverChannel.close();
                System.out.println("Server socket closed.");
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }

        try {
            if (selector != null && selector.isOpen()) {
                selector.close();
                System.out.println("Selector closed.");
            }
        } catch (IOException e) {
            System.err.println("Error closing selector: " + e.getMessage());
        }

        if (pool != null && !pool.isShutdown()) {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.out.println("Force-shutting down generator threads...");
                    pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
            System.out.println("Generator thread pool stopped.");
        }

        System.out.println("KeyServer stopped successfully.");
    }

    record PairPem(byte[] priv, byte[] cert) {}

    public KeyServer(int port, int genThreads, PrivateKey key, String issuerDN) throws IOException {
        this.port = port;
        this.genThreads = genThreads;
        this.issuerKey = key;
        this.issuerDN = issuerDN;
        this.selector = Selector.open();
        this.serverChannel = ServerSocketChannel.open();
        this.serverChannel.bind(new InetSocketAddress(port));
        this.serverChannel.configureBlocking(false);
        this.serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        this.pool = Executors.newFixedThreadPool(genThreads);
    }

    public void start() throws IOException {
        System.out.println("Server listening on port " + port);
        while (running.get()) {
            processReady();
            selector.select(500);
            processReady();
            for (Iterator<SelectionKey> it = selector.selectedKeys().iterator(); it.hasNext();) {
                SelectionKey key = it.next(); it.remove();
                if (!key.isValid()) continue;
                if (key.isAcceptable()) handleAccept();
                else if (key.isReadable()) handleRead(key);
                else if (key.isWritable()) handleWrite(key);
            }
        }
    }

    private void processReady() {
        ClientConnection c;
        while ((c = readyToWrite.poll()) != null) {
            SelectionKey k = c.channel.keyFor(selector);
            if (k != null && k.isValid()) {
                k.interestOps(SelectionKey.OP_WRITE);
            }
        }
    }

    private void handleAccept() throws IOException {
        SocketChannel sc = serverChannel.accept();
        if (sc == null) return;
        sc.configureBlocking(false);
        sc.register(selector, SelectionKey.OP_READ, new ClientConnection(sc));
        System.out.println("Accepted " + sc.getRemoteAddress());
    }

    private void handleRead(SelectionKey key) {
        ClientConnection c = (ClientConnection) key.attachment();
        try {
            int n = c.channel.read(c.readBuffer);
            if (n == -1) {
                c.close();
                key.cancel();
                return;
            }
            c.readBuffer.flip();
            byte[] data = new byte[c.readBuffer.limit()];
            c.readBuffer.get(data);
            int zero = -1;
            for (int i = 0; i < data.length; i++)
                if (data[i] == 0) {
                    zero = i;
                    break;
                }
            if (zero == -1) {
                c.readBuffer.compact();
                return;
            }

            String name = new String(data, 0, zero, StandardCharsets.US_ASCII);
            System.out.println("Request: " + name);
            c.requestedName = name;
            c.readBuffer.clear();
            key.interestOps(0);

            var fut = cache.computeIfAbsent(name, nm -> {
                var f = new CompletableFuture<PairPem>();
                pool.submit(() -> {
                    try {
                        var kp = CertificateUtils.generateRSAKeyPair(8192);
                        var cert = CertificateUtils.buildCertificate(nm, kp.getPublic(), issuerKey, issuerDN);
                        var privPem = CertificateUtils.toPem(kp.getPrivate()).getBytes(StandardCharsets.UTF_8);
                        var certPem = CertificateUtils.toPem(cert).getBytes(StandardCharsets.UTF_8);
                        f.complete(new PairPem(privPem, certPem));
                        System.out.println("Generated for " + nm);
                    } catch (Exception e) {
                        f.completeExceptionally(e);
                    }
                });
                return f;
            });

            fut.whenComplete((res, ex) -> {
                try {
                    ByteBuffer bb;
                    if (ex != null) {
                        System.err.println("Error generating key for " + c.requestedName + ": " + ex);
                        byte[] msg = ("ERROR: " + ex).getBytes(StandardCharsets.UTF_8);
                        bb = ByteBuffer.allocate(4 + msg.length).putInt(msg.length).put(msg);
                    } else {
                        System.out.println("Sending key for " + c.requestedName +
                            " (priv: " + res.priv().length + " bytes, cert: " + res.cert().length + " bytes)");
                        bb = ByteBuffer.allocate(4 + res.priv().length + 4 + res.cert().length);
                        bb.putInt(res.priv().length).put(res.priv());
                        bb.putInt(res.cert().length).put(res.cert());
                    }
                    bb.flip();
                    c.writeBuffer = bb;
                    readyToWrite.add(c);
                    selector.wakeup();
                } catch (Exception e2) {
                    System.err.println("Error in whenComplete: " + e2);
                    e2.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleWrite(SelectionKey key) {
        ClientConnection client = (ClientConnection) key.attachment();
        try {
            ByteBuffer buffer = client.writeBuffer;
            if (buffer == null) {
                key.interestOps(0);
                return;
            }

            client.channel.write(buffer);

            if (!buffer.hasRemaining()) {
                client.writeBuffer = null;
                try {
                    System.out.println("Finished sending to " + client.channel.getRemoteAddress());
                } catch (IOException ignored) {}
                client.close();
                key.cancel();
            }

        } catch (IOException e) {
            try {
                System.err.println("Write error: " + e.getMessage());
            } catch (Exception ignored) {}
            client.close();
            key.cancel();
        }
    }
}
