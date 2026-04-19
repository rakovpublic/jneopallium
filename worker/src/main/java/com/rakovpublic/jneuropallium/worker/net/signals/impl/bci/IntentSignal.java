/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.bci;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci.IntentKind;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Decoded user intent: the output of the decoder chain before it is turned
 * into motor or stimulation commands.
 * ProcessingFrequency: loop=1, epoch=1.
 */
public class IntentSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private IntentKind kind;
    private double[] parameters;
    private double confidence;

    public IntentSignal() {
        super();
        this.loop = 1;
        this.epoch = 1L;
        this.timeAlive = 10;
        this.kind = IntentKind.NONE;
    }

    public IntentSignal(IntentKind kind, double[] parameters, double confidence) {
        this();
        this.kind = kind == null ? IntentKind.NONE : kind;
        this.parameters = parameters;
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
    }

    public IntentKind getKind() { return kind; }
    public void setKind(IntentKind k) { this.kind = k == null ? IntentKind.NONE : k; }
    public double[] getParameters() { return parameters; }
    public void setParameters(double[] p) { this.parameters = p; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double c) { this.confidence = Math.max(0.0, Math.min(1.0, c)); }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return IntentSignal.class; }
    @Override public String getDescription() { return "IntentSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        double[] p = parameters == null ? null : parameters.clone();
        IntentSignal c = new IntentSignal(kind, p, confidence);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
