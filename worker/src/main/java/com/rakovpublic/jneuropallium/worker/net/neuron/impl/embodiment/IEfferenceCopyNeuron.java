package com.rakovpublic.jneuropallium.worker.net.neuron.impl.embodiment;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.signals.fast.MotorCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.EfferenceCopySignal;

public interface IEfferenceCopyNeuron extends IModulatableNeuron, IEfferenceCopyProducer {
    EfferenceCopySignal produceCopy(MotorCommandSignal motor);
}
