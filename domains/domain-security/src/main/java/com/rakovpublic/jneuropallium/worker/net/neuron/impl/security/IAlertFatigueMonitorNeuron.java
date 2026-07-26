package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.IncidentReportSignal;

public interface IAlertFatigueMonitorNeuron extends IModulatableNeuron {
    void observe(IncidentReportSignal r);
    void acknowledge(String incidentId, boolean trueAlert);
    double falsePositiveRate();
    /** Suggested multiplier (&gt;=1) that should be applied to AnomalyScoreSignal thresholds. */
    double thresholdMultiplier();
    int reports();
    int acknowledged();
}
