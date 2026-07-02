/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * The model's terminal output: a read-only maintenance advisory for a human.
 * It recommends inspection of an asset within an estimated lead time. It never
 * actuates anything — {@code advisoryOnly} is always true.
 *
 * ProcessingFrequency: loop=2, epoch=2.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MaintenanceAdvisorySignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(2L, 2);

    private String assetId;
    private String faultFamily;
    private double severity;
    private long leadTimeTicks;
    private double uncertainty;
    private String recommendation;
    private final boolean advisoryOnly = true;
    private long timestamp;

    public MaintenanceAdvisorySignal() {
        super();
        this.loop = 2;
        this.epoch = 2L;
        this.timeAlive = 0;
    }

    public MaintenanceAdvisorySignal(String assetId, String faultFamily, double severity, long leadTimeTicks,
                                     double uncertainty, String recommendation, long timestamp) {
        this();
        this.assetId = assetId;
        this.faultFamily = faultFamily;
        this.severity = severity;
        this.leadTimeTicks = leadTimeTicks;
        this.uncertainty = uncertainty;
        this.recommendation = recommendation;
        this.timestamp = timestamp;
    }

    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }
    public String getFaultFamily() { return faultFamily; }
    public void setFaultFamily(String faultFamily) { this.faultFamily = faultFamily; }
    public double getSeverity() { return severity; }
    public void setSeverity(double severity) { this.severity = severity; }
    public long getLeadTimeTicks() { return leadTimeTicks; }
    public void setLeadTimeTicks(long leadTimeTicks) { this.leadTimeTicks = leadTimeTicks; }
    public double getUncertainty() { return uncertainty; }
    public void setUncertainty(double uncertainty) { this.uncertainty = uncertainty; }
    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    public boolean isAdvisoryOnly() { return advisoryOnly; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return MaintenanceAdvisorySignal.class; }
    @Override public String getDescription() { return "MaintenanceAdvisorySignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        MaintenanceAdvisorySignal c = new MaintenanceAdvisorySignal(assetId, faultFamily, severity,
                leadTimeTicks, uncertainty, recommendation, timestamp);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
