package com.rakovpublic.jneuropallium.worker.neuron.impl.layersizing;

import com.rakovpublic.jneuropallium.worker.net.layers.ILayer;
import com.rakovpublic.jneuropallium.worker.neuron.ILayerManipulatingNeuron;
import com.rakovpublic.jneuropallium.worker.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.neuron.impl.Neuron;


public class LayerManipulatingNeuron extends Neuron implements ILayerManipulatingNeuron {

    public LayerManipulatingNeuron(Long neuronId, ISignalChain processingChain, Long run, ILayer layer) {
        super(neuronId, processingChain, run);
    }
}
