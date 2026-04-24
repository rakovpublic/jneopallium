package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.QuarantineRequestSignal;

public interface IResponseGateNeuron extends IModulatableNeuron {
    /**
     * Gate a proposed quarantine. Returns the request unchanged when SAFE,
     * null when UNCERTAIN (alert-only) or HARMFUL (vetoed by the hard
     * self-tolerance constraints). Never modifies the request in-place.
     */
    QuarantineRequestSignal gate(QuarantineRequestSignal req, double posterior);
    void registerHardAllow(String entityPattern);
    void registerCriticalAsset(String entityId);
    boolean isHardAllowed(String entityId);
    boolean isCritical(String entityId);
    String getMode();
    void setMode(String mode); // enforcing / monitor-only / alert-only
}
