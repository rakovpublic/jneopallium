/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.integration.nengo;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Inbound carrier signal — the decoded Nengo vector plus frame metadata
 * (15-NENGO.md §6.1).
 *
 * <p>This signal is intentionally <b>opaque</b> at the safety-pipeline
 * layer: planning / harm-gate / safety-mode neurons do <b>not</b> reason
 * over it directly. {@link NengoInputMapper} converts it into typed
 * {@link com.rakovpublic.jneuropallium.ai.signals.fast.SensorySignal},
 * {@link com.rakovpublic.jneuropallium.ai.signals.fast.HarmAssessmentSignal},
 * efficiency, and measurement signals before any neuron consumes the data.
 *
 * <p>The {@code transparency_log_id} field is preserved so a downstream
 * mapper can correlate emitted typed signals back to the originating
 * Nengo frame.
 */
public final class NengoDecodedStateSignal extends AbstractSignal<Void>
        implements ISignal<Void>, IInputSignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private String frameId;
    private long sequenceNo;
    private long timestampMs;
    private long validUntilMs;
    private String safetyStatus;
    private Map<String, Double> values;
    private String transparencyLogId;

    public NengoDecodedStateSignal() {
        super();
        this.loop = 1;
        this.epoch = 1L;
        this.timeAlive = 30;
        this.values = new LinkedHashMap<>();
        this.safetyStatus = NengoInputFrame.STATUS_OK;
    }

    public NengoDecodedStateSignal(
            String frameId,
            long sequenceNo,
            long timestampMs,
            long validUntilMs,
            String safetyStatus,
            Map<String, Double> values,
            String transparencyLogId) {
        this();
        this.frameId = frameId;
        this.sequenceNo = sequenceNo;
        this.timestampMs = timestampMs;
        this.validUntilMs = validUntilMs;
        this.safetyStatus = safetyStatus == null ? NengoInputFrame.STATUS_OK : safetyStatus;
        this.values = values == null ? new LinkedHashMap<>() : new LinkedHashMap<>(values);
        this.transparencyLogId = transparencyLogId;
    }

    public static NengoDecodedStateSignal fromFrame(NengoInputFrame f) {
        return new NengoDecodedStateSignal(
                f.frameId(), f.sequenceNo(), f.timestampMs(), f.validUntilMs(),
                f.safetyStatus(), f.values(), f.transparencyLogId());
    }

    public String getFrameId() { return frameId; }
    public void setFrameId(String frameId) { this.frameId = frameId; }
    public long getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(long sequenceNo) { this.sequenceNo = sequenceNo; }
    public long getTimestampMs() { return timestampMs; }
    public void setTimestampMs(long timestampMs) { this.timestampMs = timestampMs; }
    public long getValidUntilMs() { return validUntilMs; }
    public void setValidUntilMs(long validUntilMs) { this.validUntilMs = validUntilMs; }
    public String getSafetyStatus() { return safetyStatus; }
    public void setSafetyStatus(String safetyStatus) { this.safetyStatus = safetyStatus; }
    public Map<String, Double> getValues() { return Collections.unmodifiableMap(values); }
    public void setValues(Map<String, Double> values) {
        this.values = values == null ? new LinkedHashMap<>() : new LinkedHashMap<>(values);
    }
    public String getTransparencyLogId() { return transparencyLogId; }
    public void setTransparencyLogId(String transparencyLogId) { this.transparencyLogId = transparencyLogId; }

    public boolean isStale(long nowMs) { return nowMs > validUntilMs; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() {
        return NengoDecodedStateSignal.class;
    }
    @Override public String getDescription() { return "NengoDecodedStateSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        NengoDecodedStateSignal c = new NengoDecodedStateSignal(
                frameId, sequenceNo, timestampMs, validUntilMs,
                safetyStatus, values, transparencyLogId);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        c.inputName = this.inputName;
        return (K) c;
    }

    @Override public String getInputName() { return inputName; }
}
