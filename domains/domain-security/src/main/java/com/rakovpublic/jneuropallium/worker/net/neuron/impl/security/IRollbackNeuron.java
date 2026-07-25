package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.IncidentReportSignal;

public interface IRollbackNeuron extends IModulatableNeuron {
    boolean isEnabled();
    void setEnabled(boolean v);
    /** Attempt snapshot-based rollback. Returns true iff a rollback was actually initiated. */
    boolean maybeRollback(IncidentReportSignal incident);
    int attempted();
    int succeeded();
}
