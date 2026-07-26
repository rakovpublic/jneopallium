/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm.AlertCategory;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Regional swarm alert — collective harm or environmental hazard
 * detected. ProcessingFrequency: loop=1, epoch=1.
 */
public class SwarmAlertSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private AlertCategory category;
    private String regionId;
    private double severity;

    public SwarmAlertSignal() {
        super();
        this.loop = 1;
        this.epoch = 1L;
        this.timeAlive = 50;
        this.category = AlertCategory.ENVIRONMENTAL;
    }

    public SwarmAlertSignal(AlertCategory category, String regionId, double severity) {
        this();
        this.category = category == null ? AlertCategory.ENVIRONMENTAL : category;
        this.regionId = regionId;
        this.severity = Math.max(0.0, Math.min(1.0, severity));
    }

    public AlertCategory getCategory() { return category; }
    public void setCategory(AlertCategory c) { this.category = c == null ? AlertCategory.ENVIRONMENTAL : c; }
    public String getRegionId() { return regionId; }
    public void setRegionId(String r) { this.regionId = r; }
    public double getSeverity() { return severity; }
    public void setSeverity(double s) { this.severity = Math.max(0.0, Math.min(1.0, s)); }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return SwarmAlertSignal.class; }
    @Override public String getDescription() { return "SwarmAlertSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        SwarmAlertSignal c = new SwarmAlertSignal(category, regionId, severity);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
