/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * One observation of a peer — position / velocity in local frame plus a
 * link-quality indicator. Per spec §3: no perfect-comms assumption.
 * ProcessingFrequency: loop=1, epoch=2.
 */
public class PeerObservationSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(2L, 1);

    private String peerId;
    private double[] positionLocal;
    private double[] velocityLocal;
    private double linkQuality;

    public PeerObservationSignal() { super(); this.loop = 1; this.epoch = 2L; this.timeAlive = 100; }

    public PeerObservationSignal(String peerId, double[] positionLocal, double[] velocityLocal, double linkQuality) {
        this();
        this.peerId = peerId;
        this.positionLocal = positionLocal == null ? null : positionLocal.clone();
        this.velocityLocal = velocityLocal == null ? null : velocityLocal.clone();
        this.linkQuality = Math.max(0.0, Math.min(1.0, linkQuality));
    }

    public String getPeerId() { return peerId; }
    public void setPeerId(String p) { this.peerId = p; }
    public double[] getPositionLocal() { return positionLocal == null ? null : positionLocal.clone(); }
    public void setPositionLocal(double[] v) { this.positionLocal = v == null ? null : v.clone(); }
    public double[] getVelocityLocal() { return velocityLocal == null ? null : velocityLocal.clone(); }
    public void setVelocityLocal(double[] v) { this.velocityLocal = v == null ? null : v.clone(); }
    public double getLinkQuality() { return linkQuality; }
    public void setLinkQuality(double l) { this.linkQuality = Math.max(0.0, Math.min(1.0, l)); }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return PeerObservationSignal.class; }
    @Override public String getDescription() { return "PeerObservationSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        PeerObservationSignal c = new PeerObservationSignal(peerId, positionLocal, velocityLocal, linkQuality);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
