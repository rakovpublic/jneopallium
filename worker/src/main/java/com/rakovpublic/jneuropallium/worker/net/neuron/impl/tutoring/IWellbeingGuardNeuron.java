package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.InterventionSignal;

public interface IWellbeingGuardNeuron extends IModulatableNeuron {
    InterventionSignal assess(FlowStateKind state);
    int getEscalationStrikes();
    int getConsecutiveFrustrationTicks();
    int getConsecutiveBoredomTicks();
    void setMaxFrustrationTicks(int n);
    void setMaxBoredomTicks(int n);

    /**
     * Observation channel: an externally-triggered intervention is
     * recorded for audit. Default no-op.
     */
    default void observe(InterventionSignal s) { /* no-op by default */ }
}
