/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.security;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.security.Severity;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SOC-facing incident summary aggregating the evidence and response
 * actions taken. ProcessingFrequency: loop=2, epoch=1.
 */
public class IncidentReportSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 2);

    private String incidentId;
    private List<String> linkedEvents;
    private Severity severity;
    private String summary;

    public IncidentReportSignal() {
        super();
        this.loop = 2;
        this.epoch = 1L;
        this.timeAlive = 1000;
        this.severity = Severity.LOW;
        this.linkedEvents = new ArrayList<>();
    }

    public IncidentReportSignal(String incidentId, List<String> linkedEvents, Severity severity, String summary) {
        this();
        this.incidentId = incidentId;
        this.linkedEvents = linkedEvents == null ? new ArrayList<>() : new ArrayList<>(linkedEvents);
        this.severity = severity == null ? Severity.LOW : severity;
        this.summary = summary;
    }

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String i) { this.incidentId = i; }
    public List<String> getLinkedEvents() { return Collections.unmodifiableList(linkedEvents); }
    public void setLinkedEvents(List<String> l) {
        this.linkedEvents = l == null ? new ArrayList<>() : new ArrayList<>(l);
    }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity s) { this.severity = s == null ? Severity.LOW : s; }
    public String getSummary() { return summary; }
    public void setSummary(String s) { this.summary = s; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return IncidentReportSignal.class; }
    @Override public String getDescription() { return "IncidentReportSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        IncidentReportSignal c = new IncidentReportSignal(incidentId, linkedEvents, severity, summary);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
