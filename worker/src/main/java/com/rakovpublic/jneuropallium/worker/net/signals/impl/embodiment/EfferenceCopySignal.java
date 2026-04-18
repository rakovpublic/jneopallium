/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Efference copy of a motor command.
 * Biological analogue: cerebellar/parietal internal forward model
 * (Wolpert, Miall &amp; Kawato 1998).
 * <p>ProcessingFrequency: loop=1, epoch=1.
 */
public class EfferenceCopySignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private long originalMotorCommandId;
    private double[] predictedOutcome;
    private int effectorId;

    public EfferenceCopySignal() {
        super();
        this.loop = 1;
        this.epoch = 1L;
        this.timeAlive = 100;
    }

    public EfferenceCopySignal(long originalMotorCommandId, double[] predictedOutcome, int effectorId) {
        this();
        this.originalMotorCommandId = originalMotorCommandId;
        this.predictedOutcome = predictedOutcome == null ? new double[0] : predictedOutcome.clone();
        this.effectorId = effectorId;
    }

    public long getOriginalMotorCommandId() { return originalMotorCommandId; }
    public void setOriginalMotorCommandId(long originalMotorCommandId) { this.originalMotorCommandId = originalMotorCommandId; }

    public double[] getPredictedOutcome() { return predictedOutcome == null ? new double[0] : predictedOutcome.clone(); }
    public void setPredictedOutcome(double[] predictedOutcome) { this.predictedOutcome = predictedOutcome == null ? new double[0] : predictedOutcome.clone(); }

    public int getEffectorId() { return effectorId; }
    public void setEffectorId(int effectorId) { this.effectorId = effectorId; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return EfferenceCopySignal.class; }
    @Override public String getDescription() { return "EfferenceCopySignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        EfferenceCopySignal c = new EfferenceCopySignal(originalMotorCommandId, predictedOutcome, effectorId);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
