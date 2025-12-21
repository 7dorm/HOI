package ru.nsu.nocode.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientConnection {
    public final SocketChannel channel;
    public final ByteBuffer readBuffer = ByteBuffer.allocate(4096);
    public volatile ByteBuffer writeBuffer;
    public String requestedName;
    public final AtomicBoolean closed = new AtomicBoolean(false);

    public ClientConnection(SocketChannel ch) {
        this.channel = ch;
    }

    public void close() {
        if (closed.compareAndSet(false, true)) {
            try { channel.close(); } catch (IOException ignored) {}
        }
    }
}
