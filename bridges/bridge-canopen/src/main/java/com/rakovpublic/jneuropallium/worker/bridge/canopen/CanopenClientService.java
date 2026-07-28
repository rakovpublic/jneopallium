/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.canopen;

import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;

import java.util.List;

/**
 * Orchestrator surface used by {@link CanopenMeasurementInput},
 * {@link CanopenStateInput}, {@link CanopenEventInput} and
 * {@link CanopenAdvisoryOutputAggregator}. Per 13-CANOPEN.md §6 the
 * "platform module is selected at startup" — the same surface is exposed
 * by:
 *
 * <ul>
 *   <li>{@link SocketCanClientService}  — Linux JNA / SocketCAN backend
 *       (lowest latency).</li>
 *   <li>{@link UsbCanClientService}     — cross-platform Lawicel-style
 *       USB-CAN dongle backend (CANable / Korlan / PCAN).</li>
 *   <li>{@link AbstractCanopenClientService} — concrete shared
 *       orchestration that the two platform classes extend; tests use it
 *       directly via a trivial in-memory subclass.</li>
 * </ul>
 *
 * <p>The interface is intentionally narrow — bindings, decoded signals,
 * heartbeat watchdog, and audit are owned by the implementation; consumers
 * just pump the queue (read-side) or push a tag/value (write-side).
 */
public interface CanopenClientService extends AutoCloseable {

    /** Bridge name carried in every audit record (00-FRAMEWORK §4). */
    String BRIDGE_NAME = "canopen";

    /** Reconnect / cache-cleared advisory event (00-FRAMEWORK §2.3). */
    String BRIDGE_RECONNECTED = "BRIDGE_RECONNECTED";

    /** §5 heartbeat-loss threshold. */
    long HEARTBEAT_TIMEOUT_MS = 2_000L;

    /** Open the underlying CAN interface (or no-op for a stub). Idempotent. */
    void start();

    /** Drain (and clear) all decoded signals for one read or write binding. */
    List<IInputSignal> drain(String bindingId);

    /** Drain (and clear) all advisory alarm/event signals (EMCY, heartbeat-loss, reconnect). */
    List<IInputSignal> drainEvents();

    /**
     * Issue a write through the configured channel (SDO or RPDO). The
     * aggregator has already audited the verdict; this method enforces the
     * runtime backstop for forbidden indices and the per-node allow-list,
     * and returns {@code true} only on a transport-level success.
     */
    boolean send(String bindingId, double value, long ts, long run);

    /**
     * Heartbeat watchdog (§5, §10 S9). Call once per tick. For every node
     * we have ever heard a heartbeat from, if more than
     * {@link #HEARTBEAT_TIMEOUT_MS} has elapsed since the last one, emit a
     * {@code NODE_OFFLINE} alarm (once per offline transition) and mark
     * subsequent reads from that node {@code Quality.UNCERTAIN}.
     */
    void checkHeartbeats(long nowMs);

    /** Hand a frame received from the wire to the orchestrator's decoder. */
    void onCanFrame(CanFrame frame);

    /**
     * Drop the latest-value cache and emit a {@code BRIDGE_RECONNECTED}
     * advisory alarm. Called by the host after the underlying transport
     * reports a reconnect.
     */
    void onReconnected();

    CanopenBridgeConfig config();

    CanopenNodeBinding writeBindingForTag(String tag);

    CanopenNodeBinding readBinding(String bindingId);

    CanopenNodeBinding writeBinding(String bindingId);

    List<String> readBindingIds();

    @Override
    void close();
}
