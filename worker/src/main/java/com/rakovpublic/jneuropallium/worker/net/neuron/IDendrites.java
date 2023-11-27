package com.rakovpublic.jneuropallium.worker.net.neuron;

import com.rakovpublic.jneuropallium.worker.net.layers.LayerMove;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.NeuronAddress;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.HashMap;
import java.util.List;

public interface IDendrites {
    void setDefaultDendritesWeights(HashMap<Class<? extends ISignal>, IWeight> defaultDendritesWeights);

    void updateWeight(NeuronAddress neuronAddress, Class<? extends ISignal> signalClass, IWeight weight);

    List<ISignal> processSignalsWithDendrites(List<ISignal> signals);

    void removeAllWeights(NeuronAddress neuronAddress);

    void removeWeightForClass(NeuronAddress neuronAddress, Class<? extends ISignal> signalClass);

    void moveConnection(LayerMove layerMove);
}
