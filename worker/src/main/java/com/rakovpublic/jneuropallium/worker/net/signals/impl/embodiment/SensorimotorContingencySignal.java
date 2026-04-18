/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Learned action-to-sensory-change contingency.
 * Biological analogue: parieto-frontal forward model mapping (O'Regan &amp; Noe 2001).
 * <p>ProcessingFrequency: loop=1, epoch=2.
 */
public class SensorimotorContingencySignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(2L, 1);

    private int actionId;
    private double[] sensoryDelta;
    private double confidence;

    public SensorimotorContingencySignal() {
        super();
        this.loop = 1;
        this.epoch = 2L;
        this.timeAlive = 100;
    }

    public SensorimotorContingencySignal(int actionId, double[] sensoryDelta, double confidence) {
        this();
        this.actionId = actionId;
        this.sensoryDelta = sensoryDelta == null ? new double[0] : sensoryDelta.clone();
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
    }

    public int getActionId() { return actionId; }
    public void setActionId(int actionId) { this.actionId = actionId; }

    public double[] getSensoryDelta() { return sensoryDelta == null ? new double[0] : sensoryDelta.clone(); }
    public void setSensoryDelta(double[] sensoryDelta) { this.sensoryDelta = sensoryDelta == null ? new double[0] : sensoryDelta.clone(); }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = Math.max(0.0, Math.min(1.0, confidence)); }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return SensorimotorContingencySignal.class; }
    @Override public String getDescription() { return "SensorimotorContingencySignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        SensorimotorContingencySignal c = new SensorimotorContingencySignal(actionId, sensoryDelta, confidence);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
