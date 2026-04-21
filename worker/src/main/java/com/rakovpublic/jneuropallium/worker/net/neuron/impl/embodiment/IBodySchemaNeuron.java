package com.rakovpublic.jneuropallium.worker.net.neuron.impl.embodiment;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.BodySchemaUpdateSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.ProprioceptiveSignal;

public interface IBodySchemaNeuron extends IModulatableNeuron, IEmbodied {
    BodySchema currentSchema();
    void onProprioceptive(ProprioceptiveSignal s);
    void onUpdate(BodySchemaUpdateSignal update);
    void register(int effectorId, EffectorCapability cap);
}
