/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm.AnomalyKind;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Anomaly report against a peer agent. ProcessingFrequency: loop=2, epoch=1. */
public class AgentAnomalySignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 2);

    private String suspectId;
    private AnomalyKind kind;
    private String detectorId;
    private List<String> witnesses;

    public AgentAnomalySignal() {
        super();
        this.loop = 2;
        this.epoch = 1L;
        this.timeAlive = 500;
        this.kind = AnomalyKind.SILENT;
        this.witnesses = new ArrayList<>();
    }

    public AgentAnomalySignal(String suspectId, AnomalyKind kind, String detectorId, List<String> witnesses) {
        this();
        this.suspectId = suspectId;
        this.kind = kind == null ? AnomalyKind.SILENT : kind;
        this.detectorId = detectorId;
        this.witnesses = witnesses == null ? new ArrayList<>() : new ArrayList<>(witnesses);
    }

    public String getSuspectId() { return suspectId; }
    public void setSuspectId(String s) { this.suspectId = s; }
    public AnomalyKind getKind() { return kind; }
    public void setKind(AnomalyKind k) { this.kind = k == null ? AnomalyKind.SILENT : k; }
    public String getDetectorId() { return detectorId; }
    public void setDetectorId(String d) { this.detectorId = d; }
    public List<String> getWitnesses() { return Collections.unmodifiableList(witnesses); }
    public void setWitnesses(List<String> w) { this.witnesses = w == null ? new ArrayList<>() : new ArrayList<>(w); }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return AgentAnomalySignal.class; }
    @Override public String getDescription() { return "AgentAnomalySignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        AgentAnomalySignal c = new AgentAnomalySignal(suspectId, kind, detectorId, witnesses);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
