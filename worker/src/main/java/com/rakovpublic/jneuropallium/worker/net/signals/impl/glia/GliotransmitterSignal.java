/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.glia;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.glia.GliotransmitterType;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * A regional release of a gliotransmitter (glutamate, ATP, D-serine)
 * that modulates local neural activity.
 * <p>Biological analogue: tripartite-synapse gliotransmission
 * (Araque et al. 2014).
 * <p>ProcessingFrequency: loop=2, epoch=2.
 */
public class GliotransmitterSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(2L, 2);

    private GliotransmitterType transmitter;
    private double concentration;
    private int regionId;

    public GliotransmitterSignal() {
        super();
        this.loop = 2;
        this.epoch = 2L;
        this.timeAlive = 100;
    }

    public GliotransmitterSignal(GliotransmitterType transmitter, double concentration, int regionId) {
        this();
        this.transmitter = transmitter;
        this.concentration = Math.max(0.0, concentration);
        this.regionId = regionId;
    }

    public GliotransmitterType getTransmitter() { return transmitter; }
    public void setTransmitter(GliotransmitterType transmitter) { this.transmitter = transmitter; }

    public double getConcentration() { return concentration; }
    public void setConcentration(double concentration) { this.concentration = Math.max(0.0, concentration); }

    public int getRegionId() { return regionId; }
    public void setRegionId(int regionId) { this.regionId = regionId; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return GliotransmitterSignal.class; }
    @Override public String getDescription() { return "GliotransmitterSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        GliotransmitterSignal c = new GliotransmitterSignal(transmitter, concentration, regionId);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
