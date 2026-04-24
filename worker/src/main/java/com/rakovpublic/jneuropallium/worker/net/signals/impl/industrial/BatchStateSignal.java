/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.BatchPhase;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * ISA-88 batch state snapshot with key metrics. ProcessingFrequency:
 * loop=2, epoch=2.
 */
public class BatchStateSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(2L, 2);

    private String batchId;
    private BatchPhase phase;
    private Map<String, Double> keyMetrics;

    public BatchStateSignal() {
        super();
        this.loop = 2;
        this.epoch = 2L;
        this.timeAlive = 1000;
        this.phase = BatchPhase.IDLE;
        this.keyMetrics = new HashMap<>();
    }

    public BatchStateSignal(String batchId, BatchPhase phase, Map<String, Double> keyMetrics) {
        this();
        this.batchId = batchId;
        this.phase = phase == null ? BatchPhase.IDLE : phase;
        this.keyMetrics = keyMetrics == null ? new HashMap<>() : new HashMap<>(keyMetrics);
    }

    public String getBatchId() { return batchId; }
    public void setBatchId(String b) { this.batchId = b; }
    public BatchPhase getPhase() { return phase; }
    public void setPhase(BatchPhase p) { this.phase = p == null ? BatchPhase.IDLE : p; }
    public Map<String, Double> getKeyMetrics() { return Collections.unmodifiableMap(keyMetrics); }
    public void setKeyMetrics(Map<String, Double> m) {
        this.keyMetrics = m == null ? new HashMap<>() : new HashMap<>(m);
    }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return BatchStateSignal.class; }
    @Override public String getDescription() { return "BatchStateSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        BatchStateSignal c = new BatchStateSignal(batchId, phase, keyMetrics);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
