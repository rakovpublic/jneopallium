package com.rakovpublic.jneuropallium.worker.net.neuron.impl.embodiment;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.BodySchemaUpdateSignal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public interface IToolIncorporationNeuron extends IModulatableNeuron {
    BodySchemaUpdateSignal incorporate(BodySchemaNeuron target, int effectorId, EffectorCapability toolCapability);
    BodySchemaUpdateSignal release(BodySchemaNeuron target, int effectorId);
    boolean isToolEffector(int effectorId);
    int getTimeoutTicks();
    void setTimeoutTicks(int timeoutTicks);
}
