package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.ActuatorCommandSignal;

public interface ISafetyGateNeuron extends IModulatableNeuron {
    /**
     * Gate a proposed actuator command. Returns the (possibly downgraded
     * — {@code execute=false} for shadow mode) command if safe, or null
     * when the harm path vetoes.
     */
    ActuatorCommandSignal gate(ActuatorCommandSignal cmd);
    void setModeFor(String tag, SafetyMode mode);
    SafetyMode modeFor(String tag);
    SafetyMode defaultMode();
    void setDefaultMode(SafetyMode m);
}
