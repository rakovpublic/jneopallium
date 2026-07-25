package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.SelfToleranceSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.SignatureMatchSignal;

public interface IInnateInterneuron extends IModulatableNeuron {
    void onTolerance(SelfToleranceSignal s);
    /** Returns the incoming match if it should propagate, or null if suppressed by the allow-list. */
    SignatureMatchSignal filter(SignatureMatchSignal match, String entityId);
    int allowRuleCount();
    boolean isAllowed(String entityId);
}
