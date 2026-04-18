/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.sleep;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A compressed burst replay (sharp-wave ripple) emitted during NREM3
 * sleep; targets cortical long-term memory for consolidation.
 * <p>Biological analogue: hippocampal SWRs (Buzsáki 2015).
 * <p>ProcessingFrequency: loop=2, epoch=1.
 */
public class SharpWaveRippleSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 2);

    private List<Long> neuronSequence = new ArrayList<>();
    private double power;

    public SharpWaveRippleSignal() {
        super();
        this.loop = 2;
        this.epoch = 1L;
        this.timeAlive = 100;
    }

    public SharpWaveRippleSignal(List<Long> neuronSequence, double power) {
        this();
        this.neuronSequence = (neuronSequence == null) ? new ArrayList<>() : new ArrayList<>(neuronSequence);
        this.power = Math.max(0.0, power);
    }

    public List<Long> getNeuronSequence() { return Collections.unmodifiableList(neuronSequence); }
    public void setNeuronSequence(List<Long> neuronSequence) {
        this.neuronSequence = (neuronSequence == null) ? new ArrayList<>() : new ArrayList<>(neuronSequence);
    }

    public double getPower() { return power; }
    public void setPower(double power) { this.power = Math.max(0.0, power); }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return SharpWaveRippleSignal.class; }
    @Override public String getDescription() { return "SharpWaveRippleSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        SharpWaveRippleSignal c = new SharpWaveRippleSignal(neuronSequence, power);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
