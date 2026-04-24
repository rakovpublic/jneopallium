/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.security;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.security.ThreatCategory;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MITRE ATT&amp;CK-aligned threat hypothesis with its current Bayesian
 * posterior and the evidence ids that support it.
 * ProcessingFrequency: loop=2, epoch=1.
 */
public class ThreatHypothesisSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 2);

    private String hypothesisId;
    private ThreatCategory category;
    private double posterior;
    private List<String> evidenceIds;

    public ThreatHypothesisSignal() {
        super();
        this.loop = 2;
        this.epoch = 1L;
        this.timeAlive = 300;
        this.category = ThreatCategory.UNKNOWN;
        this.evidenceIds = new ArrayList<>();
    }

    public ThreatHypothesisSignal(String hypothesisId, ThreatCategory category,
                                  double posterior, List<String> evidenceIds) {
        this();
        this.hypothesisId = hypothesisId;
        this.category = category == null ? ThreatCategory.UNKNOWN : category;
        this.posterior = Math.max(0.0, Math.min(1.0, posterior));
        this.evidenceIds = evidenceIds == null ? new ArrayList<>() : new ArrayList<>(evidenceIds);
    }

    public String getHypothesisId() { return hypothesisId; }
    public void setHypothesisId(String h) { this.hypothesisId = h; }
    public ThreatCategory getCategory() { return category; }
    public void setCategory(ThreatCategory c) { this.category = c == null ? ThreatCategory.UNKNOWN : c; }
    public double getPosterior() { return posterior; }
    public void setPosterior(double p) { this.posterior = Math.max(0.0, Math.min(1.0, p)); }
    public List<String> getEvidenceIds() { return Collections.unmodifiableList(evidenceIds); }
    public void setEvidenceIds(List<String> e) {
        this.evidenceIds = e == null ? new ArrayList<>() : new ArrayList<>(e);
    }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return ThreatHypothesisSignal.class; }
    @Override public String getDescription() { return "ThreatHypothesisSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        ThreatHypothesisSignal c = new ThreatHypothesisSignal(hypothesisId, category, posterior, evidenceIds);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
