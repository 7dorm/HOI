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
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyServer {
    private static final Logger logger = LoggerFactory.getLogger(KeyServer.class);
    private static final int MAX_NAME_LENGTH = 255;
    private static final int MAX_CACHE_SIZE = 1000;
    private static final long CACHE_TTL_MS = 300_000;

    private final int port;
    private final PrivateKey issuerKey;
    private final String issuerDN;
    private final Selector selector;
    private final ServerSocketChannel serverChannel;
    private final ExecutorService pool;
    // Почитать устройство synchronizedMap
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<String, CacheEntry>() {
        private boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };
    private final ConcurrentLinkedQueue<ClientConnection> readyToWrite = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicInteger activeTasks = new AtomicInteger(0);
    private final AtomicInteger completedTasks = new AtomicInteger(0);
    private final PoolMonitor poolMonitor;

    public void shutdown() {
        logger.info("Shutting down KeyServer...");

        running.set(false);

        if (selector != null && selector.isOpen()) {
            selector.wakeup();
        }

        try {
            if (serverChannel != null && serverChannel.isOpen()) {
                serverChannel.close();
                logger.info("Server socket closed.");
            }
        } catch (IOException e) {
            logger.error("Error closing server socket: {}", e.getMessage());
        }

        try {
            if (selector != null && selector.isOpen()) {
                selector.close();
                logger.info("Selector closed.");
            }
        } catch (IOException e) {
            logger.error("Error closing selector: {}", e.getMessage());
        }

        if (poolMonitor != null) {
            poolMonitor.stop();
        }

        if (pool != null && !pool.isShutdown()) {
            logger.info("Waiting for {} active tasks to complete...", activeTasks.get());
            pool.shutdown();
            try {
                if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
                    logger.warn("Force-shutting down generator threads after timeout...");
                    pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
            logger.info("Generator thread pool stopped. Completed tasks: {}", completedTasks.get());
        }

        logger.info("KeyServer stopped successfully.");
    }

    record PairPem(byte[] priv, byte[] cert) {}

    static class CacheEntry {
        final CompletableFuture<PairPem> future;
        final long timestamp;

        CacheEntry(CompletableFuture<PairPem> future) {
            this.future = future;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }

    public KeyServer(int port, int genThreads, PrivateKey key, String issuerDN) throws IOException {
        this.port = port;
        this.issuerKey = key;
        this.issuerDN = issuerDN;
        this.selector = Selector.open();
        this.serverChannel = ServerSocketChannel.open();
        this.serverChannel.bind(new InetSocketAddress(port));
        this.serverChannel.configureBlocking(false);
        this.serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        this.pool = Executors.newFixedThreadPool(genThreads);
        this.poolMonitor = new PoolMonitor(pool, activeTasks, completedTasks, genThreads);
    }

    public void start() throws IOException {
        logger.info("Server listening on port {}", port);
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
        logger.info("Accepted connection from {}", sc.getRemoteAddress());
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

            if (name.length() > MAX_NAME_LENGTH) {
                logger.warn("Name too long: {} characters (max: {})", name.length(), MAX_NAME_LENGTH);
                sendError(c, "Name too long");
                return;
            }

            logger.info("Request: {}", name);
            c.requestedName = name;
            c.readBuffer.clear();
            key.interestOps(0);

            cleanupExpiredCache();

            var entry = cache.get(name);
            CompletableFuture<PairPem> fut;

            if (entry != null && !entry.isExpired()) {
                fut = entry.future;
                logger.debug("Using cached result for: {}", name);
            } else {
                fut = new CompletableFuture<PairPem>();
                activeTasks.incrementAndGet();
                pool.submit(() -> {
                    try {
                        KeyPair kp;
                        X509Certificate cert;

                        kp = CertificateUtils.generateRSAKeyPair(2048);
                        cert = CertificateUtils.buildCertificate(name, kp.getPublic(), issuerKey, issuerDN);

                        var privPem = CertificateUtils.toPem(kp.getPrivate()).getBytes(StandardCharsets.UTF_8);
                        var certPem = CertificateUtils.toPem(cert).getBytes(StandardCharsets.UTF_8);
                        fut.complete(new PairPem(privPem, certPem));
                        completedTasks.incrementAndGet();
                        activeTasks.decrementAndGet();
                        logger.info("Generated key pair for: {}", name);
                    } catch (Exception e) {
                        activeTasks.decrementAndGet();
                        fut.completeExceptionally(e);
                    }
                });
                cache.put(name, new CacheEntry(fut));
            }

            fut.whenComplete((res, ex) -> {
                try {
                    ByteBuffer bb;
                    if (ex != null) {
                        logger.error("Error generating key for {}: {}", c.requestedName, ex.getMessage());
                        sendError(c, "Key generation failed: " + ex.getMessage());
                    } else {
                        logger.info("Sending key for {} (priv: {} bytes, cert: {} bytes)",
                            c.requestedName, res.priv().length, res.cert().length);
                        bb = ByteBuffer.allocate(1 + 4 + res.priv().length + 4 + res.cert().length);
                        bb.put((byte) 0);
                        bb.putInt(res.priv().length).put(res.priv());
                        bb.putInt(res.cert().length).put(res.cert());
                        bb.flip();
                        c.writeBuffer = bb;
                        readyToWrite.add(c);
                        selector.wakeup();
                    }
                } catch (Exception e2) {
                    logger.error("Error in whenComplete: {}", e2.getMessage(), e2);
                }
            });
        } catch (Exception e) {
            logger.error("Error handling read: {}", e.getMessage(), e);
        }
    }

    private void sendError(ClientConnection c, String message) {
        try {
            byte[] msgBytes = message.getBytes(StandardCharsets.UTF_8);
            ByteBuffer bb = ByteBuffer.allocate(1 + 4 + msgBytes.length);
            bb.put((byte) 1);
            bb.putInt(msgBytes.length).put(msgBytes);
            bb.flip();
            c.writeBuffer = bb;
            readyToWrite.add(c);
            selector.wakeup();
        } catch (Exception e) {
            logger.error("Error sending error message: {}", e.getMessage());
        }
    }

    private void cleanupExpiredCache() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
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
                    logger.debug("Finished sending to {}", client.channel.getRemoteAddress());
                } catch (IOException ignored) {}
                client.close();
                key.cancel();
            }

        } catch (IOException e) {
            try {
                logger.error("Write error: {}", e.getMessage());
            } catch (Exception ignored) {}
            client.close();
            key.cancel();
        }
    }
}
