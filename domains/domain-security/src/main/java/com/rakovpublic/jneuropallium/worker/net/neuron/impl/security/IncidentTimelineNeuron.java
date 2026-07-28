/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.IncidentReportSignal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Layer 3 incident-timeline buffer. Groups evidence ids under an
 * incident id so downstream reporters can produce a coherent summary.
 * Loop=2 / Epoch=2.
 */
public class IncidentTimelineNeuron extends ModulatableNeuron implements IIncidentTimelineNeuron {

    private final Map<String, List<String>> events = new HashMap<>();

    public IncidentTimelineNeuron() { super(); }
    public IncidentTimelineNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override
    public void append(String incidentId, String eventId, long tick) {
        if (incidentId == null || eventId == null) return;
        events.computeIfAbsent(incidentId, k -> new ArrayList<>()).add(eventId);
    }

    @Override
    public List<String> eventsFor(String incidentId) {
        List<String> l = events.get(incidentId);
        return l == null ? Collections.emptyList() : Collections.unmodifiableList(l);
    }

    @Override
    public IncidentReportSignal summarise(String incidentId, Severity severity, String summary) {
        if (incidentId == null) return null;
        return new IncidentReportSignal(incidentId,
                events.getOrDefault(incidentId, Collections.emptyList()),
                severity, summary);
    }

    @Override public int incidentCount() { return events.size(); }
}
