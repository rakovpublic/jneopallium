/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Domain-shift estimate against the learned or site-adapted baseline. */
public class DomainShiftSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(4L, 2);

    private String assetId;
    private double domainShiftScore;
    private double uncertainty;
    private String baselineKey;
    private String observedKey;
    private List<String> evidence;
    private long timestamp;

    public DomainShiftSignal() {
        super();
        this.loop = 2;
        this.epoch = 4L;
        this.timeAlive = 1000;
        this.evidence = new ArrayList<>();
    }

    public DomainShiftSignal(String assetId, double domainShiftScore, double uncertainty,
                             String baselineKey, String observedKey, List<String> evidence,
                             long timestamp) {
        this();
        this.assetId = assetId;
        this.domainShiftScore = clamp(domainShiftScore);
        this.uncertainty = clamp(uncertainty);
        this.baselineKey = baselineKey;
        this.observedKey = observedKey;
        setEvidence(evidence);
        this.timestamp = timestamp;
    }

    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }
    public double getDomainShiftScore() { return domainShiftScore; }
    public void setDomainShiftScore(double domainShiftScore) { this.domainShiftScore = clamp(domainShiftScore); }
    public double getUncertainty() { return uncertainty; }
    public void setUncertainty(double uncertainty) { this.uncertainty = clamp(uncertainty); }
    public String getBaselineKey() { return baselineKey; }
    public void setBaselineKey(String baselineKey) { this.baselineKey = baselineKey; }
    public String getObservedKey() { return observedKey; }
    public void setObservedKey(String observedKey) { this.observedKey = observedKey; }
    public List<String> getEvidence() { return Collections.unmodifiableList(evidence); }
    public void setEvidence(List<String> evidence) {
        this.evidence = new ArrayList<>();
        if (evidence != null) {
            for (String item : evidence) {
                if (item != null && !item.isBlank()) this.evidence.add(item);
            }
        }
    }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return DomainShiftSignal.class; }
    @Override public String getDescription() { return "DomainShiftSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        DomainShiftSignal c = new DomainShiftSignal(assetId, domainShiftScore, uncertainty,
                baselineKey, observedKey, evidence, timestamp);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) return 0.0;
        return Math.max(0.0, Math.min(1.0, value));
    }
}
