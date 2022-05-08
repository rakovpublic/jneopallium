package com.rakovpublic.jneuropallium.worker.neuron;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.neuron.impl.NeuronAddress;

import java.util.List;

public interface IDendrites {
    void updateWeight(NeuronAddress neuronAddress, Class<? extends ISignal> signalClass, IWeight weight);
    List<ISignal> processSignalsWithDendrites(List<ISignal> signals);
    void removeAllWeights(NeuronAddress neuronAddress);
    void removeWeightForClass(NeuronAddress neuronAddress, Class<? extends ISignal> signalClass);
}
