package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

public interface IGripSelectionNeuron extends IModulatableNeuron {
    GripSelectionNeuron.GripType select(double sizeMeters, double elongation, boolean thin);
    GripSelectionNeuron.GripType getLastGrip();
}
