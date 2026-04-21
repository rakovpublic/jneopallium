package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.StimulationCommandSignal;

public interface IActuatorNeuron extends IModulatableNeuron {
    boolean dispatch(StimulationCommandSignal cmd, long currentTick);
    long getDispatchedCount();
    long getLastDispatchTick();
    void reset();
}
