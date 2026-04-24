package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.IncidentReportSignal;

import java.util.List;

public interface IIncidentTimelineNeuron extends IModulatableNeuron {
    void append(String incidentId, String eventId, long tick);
    List<String> eventsFor(String incidentId);
    IncidentReportSignal summarise(String incidentId, Severity severity, String summary);
    int incidentCount();
}
