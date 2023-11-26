package com.rakovpublic.jneuropallium.worker.net.neuron.impl.layersizing;

import com.rakovpublic.jneuropallium.worker.net.layers.ILayer;
import com.rakovpublic.jneuropallium.worker.net.neuron.ILayerManipulatingNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.Neuron;


public class LayerManipulatingNeuron extends Neuron implements ILayerManipulatingNeuron {

    public LayerManipulatingNeuron(Long neuronId, ISignalChain processingChain, Long run, ILayer layer) {
        super(neuronId, processingChain, run);
    }
}
