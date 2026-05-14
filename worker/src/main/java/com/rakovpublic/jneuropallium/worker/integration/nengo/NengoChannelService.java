/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.integration.nengo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Newline-delimited JSON transport for both Nengo channels
 * (15-NENGO.md §3).
 *
 * <p>One instance owns one direction. The default transport is Unix
 * Domain Socket; the FILE fallback is append-only JSONL — perfect for
 * deterministic CI replay.
 *
 * <p>In UDS mode the service is symmetric: the input-side service binds
 * a server socket and accepts a single Python writer; the output-side
 * service binds a server socket and accepts a single Python reader.
 * Reconnects are handled by the input poller calling {@link #pollFrames}
 * which transparently re-accepts. The bridge stays JDK-only.
 *
 * <p>{@link #pollFrames} is non-blocking: it returns the frames buffered
 * since the previous call. {@link #writeFrame} blocks only on the local
 * I/O write.
 */
public final class NengoChannelService implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(NengoChannelService.class);

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .disable(SerializationFeature.INDENT_OUTPUT)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /** Direction of the channel. */
    public enum Direction { READ, WRITE }

    private final Path path;
    private final NengoBridgeConfig.TransportMode mode;
    private final Direction direction;
    private final int frameMaxBytes;
    private final long reconnectBackoffMs;
    private final long reconnectMaxMs;

    private final ReentrantLock lock = new ReentrantLock();
    private final Deque<NengoInputFrame> backlog = new ArrayDeque<>();
    private final AtomicLong totalFramesRead = new AtomicLong();
    private final AtomicLong totalFramesDropped = new AtomicLong();
    private final AtomicLong totalReconnects = new AtomicLong();

    // UDS state
    private ServerSocketChannel server;
    private SocketChannel peer;
    private BufferedReader udsReader;
    private BufferedWriter udsWriter;
    private long currentBackoffMs;

    // FILE state — read mode tracks position; write mode keeps a writer open
    private BufferedReader fileReader;
    private BufferedWriter fileWriter;
    private long lastFilePosition;

    private boolean closed;

    public NengoChannelService(Path path,
                               NengoBridgeConfig.TransportMode mode,
                               Direction direction,
                               int frameMaxBytes,
                               long reconnectBackoffMs,
                               long reconnectMaxMs) {
        this.path = Objects.requireNonNull(path, "path");
        this.mode = Objects.requireNonNull(mode, "mode");
        this.direction = Objects.requireNonNull(direction, "direction");
        this.frameMaxBytes = frameMaxBytes;
        this.reconnectBackoffMs = reconnectBackoffMs;
        this.reconnectMaxMs = reconnectMaxMs;
        this.currentBackoffMs = reconnectBackoffMs;
    }

    /** Convenience factory deriving everything from a {@link NengoBridgeConfig}. */
    public static NengoChannelService inputFromConfig(NengoBridgeConfig cfg) {
        NengoBridgeConfig.TransportSection t = cfg.transport();
        return new NengoChannelService(
                Paths.get(t.channelInPath()), t.mode(), Direction.READ,
                t.frameMaxBytes(), t.reconnectBackoffMs(), t.reconnectMaxMs());
    }

    /** Convenience factory deriving everything from a {@link NengoBridgeConfig}. */
    public static NengoChannelService outputFromConfig(NengoBridgeConfig cfg) {
        NengoBridgeConfig.TransportSection t = cfg.transport();
        return new NengoChannelService(
                Paths.get(t.channelOutPath()), t.mode(), Direction.WRITE,
                t.frameMaxBytes(), t.reconnectBackoffMs(), t.reconnectMaxMs());
    }

    /* ===== reader side ===================================================== */

    /**
     * Drain any frames available since the previous call.
     *
     * <p>Never blocks waiting for new I/O. In UDS mode a missing peer
     * causes a non-blocking accept attempt with exponential backoff; the
     * accept failure path returns an empty list rather than throwing so
     * the worker tick is never stalled by transport issues (15-NENGO.md
     * §10 R1, §11 S8).
     */
    public List<NengoInputFrame> pollFrames() {
        if (direction != Direction.READ) {
            throw new IllegalStateException("pollFrames() requires Direction.READ");
        }
        lock.lock();
        try {
            if (closed) return List.of();
            if (mode == NengoBridgeConfig.TransportMode.FILE) {
                drainFile();
            } else {
                drainUds();
            }
            if (backlog.isEmpty()) return List.of();
            List<NengoInputFrame> out = new ArrayList<>(backlog);
            backlog.clear();
            return out;
        } finally {
            lock.unlock();
        }
    }

    private void drainFile() {
        try {
            if (!Files.exists(path)) return;
            if (fileReader == null) {
                fileReader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
                lastFilePosition = 0L;
            }
            String line;
            while ((line = fileReader.readLine()) != null) {
                ingestLine(line);
            }
        } catch (IOException e) {
            log.warn("Nengo channel file read failed: {}", e.getMessage());
            closeQuietly(fileReader);
            fileReader = null;
        }
    }

    private void drainUds() {
        if (udsReader == null) {
            if (!ensureAccepted()) return;
        }
        try {
            // Non-blocking-ish: rely on availability via ready() to avoid
            // blocking when the peer is alive but quiet.
            while (udsReader != null && udsReader.ready()) {
                String line = udsReader.readLine();
                if (line == null) {
                    onUdsDisconnect();
                    return;
                }
                ingestLine(line);
            }
        } catch (IOException e) {
            log.info("Nengo channel UDS read disconnected: {}", e.getMessage());
            onUdsDisconnect();
        }
    }

    private boolean ensureAccepted() {
        try {
            if (server == null) {
                server = openUdsServer(path);
                server.configureBlocking(false);
            }
            SocketChannel sc = server.accept();
            if (sc == null) {
                currentBackoffMs = Math.min(currentBackoffMs * 2, reconnectMaxMs);
                return false;
            }
            sc.configureBlocking(true);
            this.peer = sc;
            this.udsReader = new BufferedReader(
                    new java.io.InputStreamReader(
                            Channels.newInputStream(sc), StandardCharsets.UTF_8));
            this.currentBackoffMs = reconnectBackoffMs;
            this.totalReconnects.incrementAndGet();
            log.info("Nengo channel UDS accepted reader on {}", path);
            return true;
        } catch (IOException e) {
            log.warn("Nengo channel UDS accept failed: {}", e.getMessage());
            return false;
        }
    }

    private void onUdsDisconnect() {
        closeQuietly(udsReader);
        closeQuietly(peer);
        udsReader = null;
        peer = null;
    }

    private void ingestLine(String line) {
        if (line.isEmpty()) return;
        byte[] raw = line.getBytes(StandardCharsets.UTF_8);
        if (raw.length > frameMaxBytes) {
            totalFramesDropped.incrementAndGet();
            log.warn("Nengo input frame exceeds frameMaxBytes ({} > {})",
                    raw.length, frameMaxBytes);
            return;
        }
        try {
            NengoInputFrame f = MAPPER.readValue(line, NengoInputFrame.class);
            backlog.addLast(f);
            totalFramesRead.incrementAndGet();
        } catch (IOException e) {
            totalFramesDropped.incrementAndGet();
            log.warn("Nengo input frame parse failed: {}", e.getMessage());
        }
    }

    /* ===== writer side ===================================================== */

    /** Write one output frame. Returns {@code true} on success. */
    public boolean writeFrame(NengoOutputFrame frame) {
        if (direction != Direction.WRITE) {
            throw new IllegalStateException("writeFrame() requires Direction.WRITE");
        }
        Objects.requireNonNull(frame, "frame");
        lock.lock();
        try {
            if (closed) return false;
            String json;
            try {
                json = MAPPER.writeValueAsString(frame);
            } catch (JsonProcessingException e) {
                log.warn("Nengo output frame serialize failed: {}", e.getMessage());
                return false;
            }
            byte[] raw = json.getBytes(StandardCharsets.UTF_8);
            if (raw.length > frameMaxBytes) {
                log.warn("Nengo output frame exceeds frameMaxBytes ({} > {})",
                        raw.length, frameMaxBytes);
                return false;
            }
            if (mode == NengoBridgeConfig.TransportMode.FILE) {
                return writeFileLine(json);
            }
            return writeUdsLine(json);
        } finally {
            lock.unlock();
        }
    }

    private boolean writeFileLine(String json) {
        try {
            if (fileWriter == null) {
                if (path.getParent() != null) Files.createDirectories(path.getParent());
                fileWriter = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
            fileWriter.write(json);
            fileWriter.newLine();
            fileWriter.flush();
            return true;
        } catch (IOException e) {
            log.warn("Nengo channel file write failed: {}", e.getMessage());
            closeQuietly(fileWriter);
            fileWriter = null;
            return false;
        }
    }

    private boolean writeUdsLine(String json) {
        if (udsWriter == null) {
            if (!ensureWriterAccepted()) return false;
        }
        try {
            udsWriter.write(json);
            udsWriter.newLine();
            udsWriter.flush();
            return true;
        } catch (IOException e) {
            log.info("Nengo channel UDS writer disconnected: {}", e.getMessage());
            closeQuietly(udsWriter);
            closeQuietly(peer);
            udsWriter = null;
            peer = null;
            return false;
        }
    }

    private boolean ensureWriterAccepted() {
        try {
            if (server == null) {
                server = openUdsServer(path);
                server.configureBlocking(false);
            }
            SocketChannel sc = server.accept();
            if (sc == null) return false;
            sc.configureBlocking(true);
            this.peer = sc;
            this.udsWriter = new BufferedWriter(
                    new java.io.OutputStreamWriter(
                            Channels.newOutputStream(sc), StandardCharsets.UTF_8));
            this.totalReconnects.incrementAndGet();
            log.info("Nengo channel UDS accepted writer on {}", path);
            return true;
        } catch (IOException e) {
            log.warn("Nengo channel UDS accept failed: {}", e.getMessage());
            return false;
        }
    }

    /* ===== metrics ========================================================= */

    public long totalFramesRead()    { return totalFramesRead.get(); }
    public long totalFramesDropped() { return totalFramesDropped.get(); }
    public long totalReconnects()    { return totalReconnects.get(); }
    public Path path()               { return path; }
    public Direction direction()     { return direction; }
    public NengoBridgeConfig.TransportMode mode() { return mode; }

    /* ===== plumbing ======================================================== */

    private static ServerSocketChannel openUdsServer(Path path) throws IOException {
        try { Files.deleteIfExists(path); } catch (IOException ignored) { /* stale socket */ }
        if (path.getParent() != null) Files.createDirectories(path.getParent());
        ServerSocketChannel s = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
        s.bind(UnixDomainSocketAddress.of(path));
        return s;
    }

    private static void closeQuietly(AutoCloseable c) {
        if (c == null) return;
        try { c.close(); } catch (Exception ignored) { /* best-effort */ }
    }

    @Override
    public void close() {
        lock.lock();
        try {
            if (closed) return;
            closed = true;
            closeQuietly(udsReader); udsReader = null;
            closeQuietly(udsWriter); udsWriter = null;
            closeQuietly(peer);      peer = null;
            closeQuietly(server);    server = null;
            closeQuietly(fileReader); fileReader = null;
            closeQuietly(fileWriter); fileWriter = null;
            if (mode == NengoBridgeConfig.TransportMode.UDS) {
                try { Files.deleteIfExists(path); } catch (IOException ignored) {}
            }
        } finally {
            lock.unlock();
        }
    }
}
