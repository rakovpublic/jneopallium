/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Fused, label-free maintenance hypothesis for one asset: the accumulated
 * evidence that maintenance is becoming necessary, the heuristically attributed
 * fault family, the severity (in own-history percentile units), an estimated
 * lead time to limit from trend extrapolation, and an uncertainty that rises
 * with domain shift. This is advisory evidence, not an actuation command.
 *
 * ProcessingFrequency: loop=2, epoch=2.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HealthHypothesisSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(2L, 2);

    private String assetId;
    private String faultFamily;
    private double severity;
    private double evidence;
    private long leadTimeTicks;
    private double uncertainty;
    private double domainShift;
    private long timestamp;

    public HealthHypothesisSignal() {
        super();
        this.loop = 2;
        this.epoch = 2L;
        this.timeAlive = 0;
    }

    public HealthHypothesisSignal(String assetId, String faultFamily, double severity, double evidence,
                                  long leadTimeTicks, double uncertainty, double domainShift, long timestamp) {
        this();
        this.assetId = assetId;
        this.faultFamily = faultFamily;
        this.severity = severity;
        this.evidence = evidence;
        this.leadTimeTicks = leadTimeTicks;
        this.uncertainty = uncertainty;
        this.domainShift = domainShift;
        this.timestamp = timestamp;
    }

    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }
    public String getFaultFamily() { return faultFamily; }
    public void setFaultFamily(String faultFamily) { this.faultFamily = faultFamily; }
    public double getSeverity() { return severity; }
    public void setSeverity(double severity) { this.severity = severity; }
    public double getEvidence() { return evidence; }
    public void setEvidence(double evidence) { this.evidence = evidence; }
    public long getLeadTimeTicks() { return leadTimeTicks; }
    public void setLeadTimeTicks(long leadTimeTicks) { this.leadTimeTicks = leadTimeTicks; }
    public double getUncertainty() { return uncertainty; }
    public void setUncertainty(double uncertainty) { this.uncertainty = uncertainty; }
    public double getDomainShift() { return domainShift; }
    public void setDomainShift(double domainShift) { this.domainShift = domainShift; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return HealthHypothesisSignal.class; }
    @Override public String getDescription() { return "HealthHypothesisSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        HealthHypothesisSignal c = new HealthHypothesisSignal(assetId, faultFamily, severity, evidence,
                leadTimeTicks, uncertainty, domainShift, timestamp);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
