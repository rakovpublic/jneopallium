/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.curiosity;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Rate of change of prediction error in a given domain. A decreasing
 * error trajectory (negative derivative) indicates ongoing learning
 * progress and yields intrinsic reward (Oudeyer &amp; Kaplan 2007).
 * <p>ProcessingFrequency: loop=2, epoch=2.
 */
public class LearningProgressSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(2L, 2);

    private String domain;
    private double errorDerivative;

    public LearningProgressSignal() {
        super();
        this.loop = 2;
        this.epoch = 2L;
        this.timeAlive = 100;
    }

    public LearningProgressSignal(String domain, double errorDerivative) {
        this();
        this.domain = domain;
        this.errorDerivative = errorDerivative;
    }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public double getErrorDerivative() { return errorDerivative; }
    public void setErrorDerivative(double errorDerivative) { this.errorDerivative = errorDerivative; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return LearningProgressSignal.class; }
    @Override public String getDescription() { return "LearningProgressSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        LearningProgressSignal c = new LearningProgressSignal(domain, errorDerivative);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
