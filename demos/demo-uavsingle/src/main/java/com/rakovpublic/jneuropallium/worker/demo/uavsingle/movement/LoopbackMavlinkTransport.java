package com.rakovpublic.jneuropallium.worker.demo.uavsingle.movement;

import com.rakovpublic.jneuropallium.worker.bridge.mavlink.MavlinkTransport;

import java.util.ArrayList;
import java.util.List;

/**
 * In-process {@link MavlinkTransport} used by {@link UavFpvAutonomyRun} to exercise the full
 * {@code sim -> MAVLink -> jneopallium -> OutputAggregator -> MAVLink -> sim} loop without a live
 * SITL / UDP wire. The synthetic sim {@link #deliver}s telemetry to the bridge, and the bridge's
 * advisory egress is captured in {@link #sent()} so the sim can apply it.
 *
 * <p>It is the seam the live UDP/TCP transport drops into unchanged: {@code UavFpvAutonomyRun}
 * builds the {@code MavlinkClientService} the same way for both.
 */
public final class LoopbackMavlinkTransport implements MavlinkTransport {

    /** One advisory egress captured from the bridge. */
    public record Egress(int systemId, int componentId, Object payload) {}

    private final List<Egress> sent = new ArrayList<>();
    private MessageHandler handler;
    private boolean connected;

    @Override
    public void connect() {
        connected = true;
    }

    @Override
    public void setHandler(MessageHandler handler) {
        this.handler = handler;
    }

    @Override
    public void send(int systemId, int componentId, Object payload) {
        sent.add(new Egress(systemId, componentId, payload));
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void close() {
        connected = false;
    }

    /** Simulate inbound telemetry from the (synthetic) autopilot. */
    public void deliver(int systemId, int componentId, Object payload) {
        if (handler != null) {
            handler.onMessage(new InboundMessage(systemId, componentId, payload));
        }
    }

    /** Drain (and clear) the advisory commands the bridge has emitted toward the sim. */
    public List<Egress> drainSent() {
        List<Egress> snapshot = new ArrayList<>(sent);
        sent.clear();
        return snapshot;
    }
}
