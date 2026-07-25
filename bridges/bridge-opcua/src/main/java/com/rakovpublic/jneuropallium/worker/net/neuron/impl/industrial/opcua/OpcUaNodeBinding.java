/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua;

import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

/**
 * Runtime projection of a {@link OpcUaBridgeConfig.NodeBindingConfig} —
 * holds the parsed {@link NodeId}, the latest cached {@link DataValue}
 * (for diff-suppression on writes), and the rate-limit history.
 */
public final class OpcUaNodeBinding {

    public final String loopId;
    public final String signalTag;
    public final NodeId nodeId;
    public final OpcUaBridgeConfig.NodeBindingConfig config;

    private volatile DataValue lastSeen;
    private volatile double lastWritten = Double.NaN;
    private volatile long lastWrittenAt;

    public OpcUaNodeBinding(OpcUaBridgeConfig.NodeBindingConfig cfg) {
        this.loopId = cfg.loopId();
        this.signalTag = cfg.signalTag();
        this.nodeId = NodeId.parse(cfg.nodeId());
        this.config = cfg;
    }

    public DataValue getLastSeen() { return lastSeen; }
    public void setLastSeen(DataValue v) { this.lastSeen = v; }

    public double getLastWritten() { return lastWritten; }
    public long getLastWrittenAt() { return lastWrittenAt; }

    public synchronized void recordWrite(double value, long ts) {
        this.lastWritten = value;
        this.lastWrittenAt = ts;
    }
}
