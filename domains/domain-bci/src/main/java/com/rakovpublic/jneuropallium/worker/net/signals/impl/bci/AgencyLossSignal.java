/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.bci;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Emitted when efference copy differs from delivered sensory feedback or
 * actual proprioception by more than the configured tolerance. Surfaces both
 * a hardware-fault channel and a user-experience metric: the signal is the
 * mirror image of the biological sense of agency.
 * ProcessingFrequency: loop=1, epoch=2.
 */
public class AgencyLossSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(2L, 1);

    private double mismatchMagnitude;
    private String modality;

    public AgencyLossSignal() {
        super();
        this.loop = 1;
        this.epoch = 2L;
        this.timeAlive = 100;
    }

    public AgencyLossSignal(double mismatchMagnitude, String modality) {
        this();
        this.mismatchMagnitude = Math.max(0.0, mismatchMagnitude);
        this.modality = modality;
    }

    public double getMismatchMagnitude() { return mismatchMagnitude; }
    public void setMismatchMagnitude(double m) { this.mismatchMagnitude = Math.max(0.0, m); }
    public String getModality() { return modality; }
    public void setModality(String m) { this.modality = m; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return AgencyLossSignal.class; }
    @Override public String getDescription() { return "AgencyLossSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        AgencyLossSignal c = new AgencyLossSignal(mismatchMagnitude, modality);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
