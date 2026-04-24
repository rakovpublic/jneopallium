/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.security;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.security.NetworkTuple;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Summary of one packet or flow. The {@code summary} blob is a
 * truncated-or-hashed payload digest — full captures stay off the
 * wire.
 * ProcessingFrequency: loop=1, epoch=1.
 */
public class PacketSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private byte[] summary;
    private NetworkTuple tuple;
    private long timestamp;

    public PacketSignal() { super(); this.loop = 1; this.epoch = 1L; this.timeAlive = 30; }

    public PacketSignal(byte[] summary, NetworkTuple tuple, long timestamp) {
        this();
        this.summary = summary == null ? null : summary.clone();
        this.tuple = tuple;
        this.timestamp = timestamp;
    }

    public byte[] getSummary() { return summary == null ? null : summary.clone(); }
    public void setSummary(byte[] s) { this.summary = s == null ? null : s.clone(); }
    public NetworkTuple getTuple() { return tuple; }
    public void setTuple(NetworkTuple t) { this.tuple = t; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long t) { this.timestamp = t; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return PacketSignal.class; }
    @Override public String getDescription() { return "PacketSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        PacketSignal c = new PacketSignal(summary, tuple, timestamp);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
