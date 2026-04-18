/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Proprioceptive feedback from a single effector.
 * Biological analogue: muscle spindle, Golgi tendon and joint-receptor
 * afferents ascending via the dorsal columns.
 * <p>ProcessingFrequency: loop=1, epoch=1.
 */
public class ProprioceptiveSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private int effectorId;
    private double[] jointStates;
    private long timestamp;

    public ProprioceptiveSignal() {
        super();
        this.loop = 1;
        this.epoch = 1L;
        this.timeAlive = 100;
    }

    public ProprioceptiveSignal(int effectorId, double[] jointStates, long timestamp) {
        this();
        this.effectorId = effectorId;
        this.jointStates = jointStates == null ? new double[0] : jointStates.clone();
        this.timestamp = timestamp;
    }

    public int getEffectorId() { return effectorId; }
    public void setEffectorId(int effectorId) { this.effectorId = effectorId; }

    public double[] getJointStates() { return jointStates == null ? new double[0] : jointStates.clone(); }
    public void setJointStates(double[] jointStates) { this.jointStates = jointStates == null ? new double[0] : jointStates.clone(); }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return ProprioceptiveSignal.class; }
    @Override public String getDescription() { return "ProprioceptiveSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        ProprioceptiveSignal c = new ProprioceptiveSignal(effectorId, jointStates, timestamp);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
