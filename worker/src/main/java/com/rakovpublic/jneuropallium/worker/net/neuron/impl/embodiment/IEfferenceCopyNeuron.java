package com.rakovpublic.jneuropallium.worker.net.neuron.impl.embodiment;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.signals.fast.MotorCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.EfferenceCopySignal;
import java.util.concurrent.atomic.AtomicLong;

public interface IEfferenceCopyNeuron extends IModulatableNeuron, IEfferenceCopyProducer {
    EfferenceCopySignal produceCopy(MotorCommandSignal motor);
}
