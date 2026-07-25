package com.rakovpublic.jneuropallium.worker.net.neuron.impl.embodiment;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.BodySchemaUpdateSignal;

public interface IToolIncorporationNeuron extends IModulatableNeuron {
    BodySchemaUpdateSignal incorporate(BodySchemaNeuron target, int effectorId, EffectorCapability toolCapability);
    BodySchemaUpdateSignal release(BodySchemaNeuron target, int effectorId);
    boolean isToolEffector(int effectorId);
    int getTimeoutTicks();
    void setTimeoutTicks(int timeoutTicks);
}
