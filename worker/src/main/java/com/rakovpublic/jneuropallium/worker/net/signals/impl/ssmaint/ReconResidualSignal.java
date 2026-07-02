/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Output of the self-supervised cross-sensor reconstruction. Carries the total
 * standardized reconstruction error for the frame plus the per-sensor residuals
 * (a sensor that its peers cannot predict shows up here) and the observed
 * domain-shift fraction. No labels were used to produce any of this.
 *
 * ProcessingFrequency: loop=1, epoch=1.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReconResidualSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private String assetId;
    private int regime;
    private double total;
    private double domainShift;
    private Map<String, Double> residuals;
    private long timestamp;

    public ReconResidualSignal() {
        super();
        this.loop = 1;
        this.epoch = 1L;
        this.timeAlive = 0;
        this.residuals = new LinkedHashMap<>();
    }

    public ReconResidualSignal(String assetId, int regime, double total, double domainShift,
                               Map<String, Double> residuals, long timestamp) {
        this();
        this.assetId = assetId;
        this.regime = regime;
        this.total = total;
        this.domainShift = domainShift;
        setResiduals(residuals);
        this.timestamp = timestamp;
    }

    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }
    public int getRegime() { return regime; }
    public void setRegime(int regime) { this.regime = regime; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public double getDomainShift() { return domainShift; }
    public void setDomainShift(double domainShift) { this.domainShift = domainShift; }
    public Map<String, Double> getResiduals() { return Collections.unmodifiableMap(residuals); }
    public void setResiduals(Map<String, Double> residuals) {
        this.residuals = new LinkedHashMap<>();
        if (residuals == null) return;
        for (Map.Entry<String, Double> e : residuals.entrySet()) {
            if (e.getKey() != null && e.getValue() != null && Double.isFinite(e.getValue())) {
                this.residuals.put(e.getKey(), e.getValue());
            }
        }
    }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return ReconResidualSignal.class; }
    @Override public String getDescription() { return "ReconResidualSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        ReconResidualSignal c = new ReconResidualSignal(assetId, regime, total, domainShift, residuals, timestamp);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
