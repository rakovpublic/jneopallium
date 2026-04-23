package com.rakovpublic.jneuropallium.worker.net.neuron.impl.embodiment;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.BodySchemaUpdateSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.ProprioceptiveSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.SensorimotorContingencySignal;

public interface IBodySchemaNeuron extends IModulatableNeuron, IEmbodied {
    BodySchema currentSchema();
    void onProprioceptive(ProprioceptiveSignal s);
    void onUpdate(BodySchemaUpdateSignal update);
    void register(int effectorId, EffectorCapability cap);

    /**
     * Observe an action → sensory-delta pairing. Default is a no-op so
     * that existing implementations are not disturbed; implementations
     * that care about contingencies may override to refine the schema.
     */
    default void onContingency(SensorimotorContingencySignal s) { /* no-op by default */ }
}
