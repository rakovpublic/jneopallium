package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.ActuatorCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.SetpointSignal;

public interface IMPCPlanningNeuron extends IModulatableNeuron {
    void setHorizon(int horizonTicks);
    void setControlHorizon(int controlHorizonTicks);
    void setProcessModel(IProcessModelNeuron model);
    /** Solve one MPC step for {@code sp} against {@code currentValue}; returns the first control move. */
    ActuatorCommandSignal step(SetpointSignal sp, double currentValue);
    int getHorizon();
    int getControlHorizon();
}
