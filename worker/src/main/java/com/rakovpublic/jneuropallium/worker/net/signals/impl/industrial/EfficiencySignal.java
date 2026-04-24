/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Per-unit efficiency measurement with its baseline.
 * ProcessingFrequency: loop=2, epoch=1.
 */
public class EfficiencySignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 2);

    private String unitId;
    private double efficiency;
    private double baseline;

    public EfficiencySignal() { super(); this.loop = 2; this.epoch = 1L; this.timeAlive = 300; }

    public EfficiencySignal(String unitId, double efficiency, double baseline) {
        this();
        this.unitId = unitId;
        this.efficiency = efficiency;
        this.baseline = baseline;
    }

    public String getUnitId() { return unitId; }
    public void setUnitId(String u) { this.unitId = u; }
    public double getEfficiency() { return efficiency; }
    public void setEfficiency(double e) { this.efficiency = e; }
    public double getBaseline() { return baseline; }
    public void setBaseline(double b) { this.baseline = b; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return EfficiencySignal.class; }
    @Override public String getDescription() { return "EfficiencySignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        EfficiencySignal c = new EfficiencySignal(unitId, efficiency, baseline);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
