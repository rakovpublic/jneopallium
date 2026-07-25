package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.QuarantineRequestSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.ThreatHypothesisSignal;

public interface IResponsePlanningNeuron extends IModulatableNeuron {
    /** Map a hypothesis to a graduated response plan; returns a quarantine request or null when only alerting applies. */
    QuarantineRequestSignal plan(ThreatHypothesisSignal t, String entityId, EntityKind kind);
    ResponseBand band(double posterior);
    void setDefaultDurationTicks(int t);
    int getDefaultDurationTicks();
    void setMaxDurationTicks(int t);
    int getMaxDurationTicks();
}
