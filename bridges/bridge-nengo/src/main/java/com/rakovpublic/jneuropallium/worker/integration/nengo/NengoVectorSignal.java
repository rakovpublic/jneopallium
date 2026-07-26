/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.integration.nengo;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Outbound carrier — labeled output vector for the Nengo realization side
 * (15-NENGO.md §6.1).
 *
 * <p>Used internally between {@link JneopalliumToNengoMapper} and
 * {@link NengoBridgeOutputAggregator}: the mapper builds one of these
 * from an approved {@code MotorCommandSignal} / {@code ApprovedActionSignal}
 * and the aggregator turns it into a {@link NengoOutputFrame} written to
 * Channel B.
 */
public final class NengoVectorSignal extends AbstractSignal<Void>
        implements ISignal<Void>, IResultSignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private String safetyStatus;
    private Map<String, Double> values;
    private String transparencyLogId;

    public NengoVectorSignal() {
        super();
        this.loop = 1;
        this.epoch = 1L;
        this.timeAlive = 30;
        this.values = new LinkedHashMap<>();
        this.safetyStatus = NengoOutputFrame.STATUS_OK;
    }

    public NengoVectorSignal(String safetyStatus,
                             Map<String, Double> values,
                             String transparencyLogId) {
        this();
        this.safetyStatus = safetyStatus == null ? NengoOutputFrame.STATUS_OK : safetyStatus;
        this.values = values == null ? new LinkedHashMap<>() : new LinkedHashMap<>(values);
        this.transparencyLogId = transparencyLogId;
    }

    public String getSafetyStatus() { return safetyStatus; }
    public void setSafetyStatus(String safetyStatus) { this.safetyStatus = safetyStatus; }
    public Map<String, Double> getValues() { return Collections.unmodifiableMap(values); }
    public void setValues(Map<String, Double> values) {
        this.values = values == null ? new LinkedHashMap<>() : new LinkedHashMap<>(values);
    }
    public String getTransparencyLogId() { return transparencyLogId; }
    public void setTransparencyLogId(String transparencyLogId) { this.transparencyLogId = transparencyLogId; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() {
        return NengoVectorSignal.class;
    }
    @Override public String getDescription() { return "NengoVectorSignal"; }
    @Override public Void getResultObject() { return null; }
    @Override public Class<Void> getResultObjectClass() { return Void.class; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        NengoVectorSignal c = new NengoVectorSignal(safetyStatus, values, transparencyLogId);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
