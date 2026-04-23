/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.AlertSeverity;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Real-time alert for an observed adverse event (guardrail excursion,
 * arrhythmia, critical lab value, early-warning score crossing).
 * ProcessingFrequency: loop=1, epoch=1.
 */
public class AdverseEventAlertSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private AlertSeverity severity;
    private String eventCode;
    private String patientId;
    private String detail;

    public AdverseEventAlertSignal() {
        super();
        this.loop = 1;
        this.epoch = 1L;
        this.timeAlive = 50;
        this.severity = AlertSeverity.INFO;
    }

    public AdverseEventAlertSignal(AlertSeverity severity, String eventCode, String patientId) {
        this();
        this.severity = severity == null ? AlertSeverity.INFO : severity;
        this.eventCode = eventCode;
        this.patientId = patientId;
    }

    public AlertSeverity getSeverity() { return severity; }
    public void setSeverity(AlertSeverity s) { this.severity = s == null ? AlertSeverity.INFO : s; }
    public String getEventCode() { return eventCode; }
    public void setEventCode(String c) { this.eventCode = c; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String p) { this.patientId = p; }
    public String getDetail() { return detail; }
    public void setDetail(String d) { this.detail = d; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return AdverseEventAlertSignal.class; }
    @Override public String getDescription() { return "AdverseEventAlertSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        AdverseEventAlertSignal c = new AdverseEventAlertSignal(severity, eventCode, patientId);
        c.detail = this.detail;
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
