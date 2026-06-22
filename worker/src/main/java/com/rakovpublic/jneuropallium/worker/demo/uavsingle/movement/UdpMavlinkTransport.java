package com.rakovpublic.jneuropallium.worker.demo.uavsingle.movement;

import com.rakovpublic.jneuropallium.worker.bridge.mavlink.MavlinkTransport;
import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;
import io.dronefleet.mavlink.minimal.Heartbeat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Real MAVLink-over-UDP {@link MavlinkTransport} — the live wire that {@link UavFpvAutonomyRun}
 * uses in {@code --mode live}. It binds a UDP port, parses inbound MAVLink (one message per
 * datagram, dronefleet common dialect) into typed payloads, and serializes outbound payloads back
 * to the remote endpoint (learned from the first inbound packet, or configured).
 *
 * <p>Each datagram is parsed with a throwaway {@link MavlinkConnection} over a
 * {@link ByteArrayInputStream}, and each send encodes with a throwaway connection over a
 * {@link ByteArrayOutputStream}; this keeps the codec stateless and thread-safe without piped
 * streams. The thin CARLA-Air bridge is the peer on the other end of the socket.
 */
public final class UdpMavlinkTransport implements MavlinkTransport {

    private final int bindPort;
    private final AtomicReference<SocketAddress> remote = new AtomicReference<>();

    private volatile DatagramSocket socket;
    private volatile MessageHandler handler;
    private volatile boolean running;
    private Thread receiver;

    public UdpMavlinkTransport(int bindPort) {
        this(bindPort, null, 0);
    }

    public UdpMavlinkTransport(int bindPort, String remoteHost, int remotePort) {
        this.bindPort = bindPort;
        if (remoteHost != null && remotePort > 0) {
            this.remote.set(new InetSocketAddress(remoteHost, remotePort));
        }
    }

    @Override
    public void connect() {
        if (running) {
            return;
        }
        try {
            socket = new DatagramSocket(bindPort);
        } catch (IOException e) {
            throw new MavlinkTransportException("UDP bind failed on port " + bindPort, e);
        }
        running = true;
        receiver = new Thread(this::receiveLoop, "udp-mavlink-rx-" + bindPort);
        receiver.setDaemon(true);
        receiver.start();
    }

    @Override
    public void setHandler(MessageHandler handler) {
        this.handler = handler;
    }

    private void receiveLoop() {
        byte[] buffer = new byte[65535];
        while (running) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                if (running) {
                    continue;
                }
                return;
            }
            remote.compareAndSet(null, packet.getSocketAddress());
            MessageHandler current = handler;
            if (current == null) {
                continue;
            }
            try (InputStream in = new ByteArrayInputStream(packet.getData(), 0, packet.getLength())) {
                MavlinkConnection connection = MavlinkConnection.create(in, OutputStream.nullOutputStream());
                MavlinkMessage<?> message;
                while ((message = nextOrNull(connection)) != null) {
                    current.onMessage(new InboundMessage(
                            message.getOriginSystemId(), message.getOriginComponentId(), message.getPayload()));
                }
            } catch (IOException ignored) {
                // Skip a malformed datagram; the wire resyncs on the next one.
            }
        }
    }

    private static MavlinkMessage<?> nextOrNull(MavlinkConnection connection) {
        try {
            return connection.next();
        } catch (IOException endOfDatagram) {
            return null;
        }
    }

    @Override
    public void send(int systemId, int componentId, Object payload) {
        SocketAddress destination = remote.get();
        DatagramSocket current = socket;
        if (destination == null || current == null || !running) {
            return;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MavlinkConnection connection = MavlinkConnection.create(InputStream.nullInputStream(), out);
            connection.send2(systemId, componentId, payload);
            byte[] data = out.toByteArray();
            current.send(new DatagramPacket(data, data.length, destination));
        } catch (IOException e) {
            throw new MavlinkTransportException("UDP MAVLink send failed", e);
        }
    }

    @Override
    public boolean isConnected() {
        return running && socket != null && !socket.isClosed();
    }

    @Override
    public void close() {
        running = false;
        DatagramSocket current = socket;
        if (current != null) {
            current.close();
        }
        if (receiver != null) {
            receiver.interrupt();
        }
    }

    /** Loopback self-test: two transports exchange a real MAVLink HEARTBEAT over UDP. */
    public static void main(String[] args) throws Exception {
        UdpMavlinkTransport a = new UdpMavlinkTransport(15010, "127.0.0.1", 15011);
        UdpMavlinkTransport b = new UdpMavlinkTransport(15011, "127.0.0.1", 15010);
        final boolean[] received = {false};
        b.setHandler(message -> {
            if (message.payload() instanceof Heartbeat) {
                received[0] = true;
                System.out.println("self-test: received HEARTBEAT from system " + message.systemId());
            }
        });
        a.connect();
        b.connect();
        a.send(7, 1, Heartbeat.builder().build());
        long deadline = System.currentTimeMillis() + 2000;
        while (!received[0] && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        a.close();
        b.close();
        System.out.println("UDP MAVLink loopback self-test: " + (received[0] ? "PASS" : "FAIL"));
        System.exit(received[0] ? 0 : 1);
    }
}
