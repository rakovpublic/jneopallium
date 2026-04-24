package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.ActuatorCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.OperatorOverrideSignal;

public interface IActuatorNeuron extends IModulatableNeuron {
    void onOverride(OperatorOverrideSignal o);
    void clearOverride(String tag);
    boolean isOverridden(String tag);
    OverrideKind overrideKind(String tag);
    /**
     * Write the command to the field (returns true iff written). Honours
     * any active operator override — MANUAL freezes at the manual value,
     * BYPASS drops the command.
     */
    boolean dispatch(ActuatorCommandSignal cmd);
    long getDispatched();
    long getBlocked();
    Double lastDispatchedValue(String tag);
}
