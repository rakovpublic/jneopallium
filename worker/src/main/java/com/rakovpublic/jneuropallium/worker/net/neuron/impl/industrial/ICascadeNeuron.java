package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.ActuatorCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.SetpointSignal;

public interface ICascadeNeuron extends IModulatableNeuron {
    /** Route the outer-PID actuator output as setpoint for the inner loop. */
    SetpointSignal forward(ActuatorCommandSignal outerOutput, String innerTag);
    /** Temporarily break the cascade so the inner loop freezes on its last-good setpoint. */
    void setBroken(boolean broken);
    boolean isBroken();
    SetpointSignal lastInnerSetpoint();
}
