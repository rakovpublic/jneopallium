package com.rakovpublic.jneuropallium.worker.neuron.impl.layersizing;

import com.rakovpublic.jneuropallium.worker.net.layers.ILayer;
import com.rakovpublic.jneuropallium.worker.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.neuron.impl.Neuron;

import java.util.List;

public class LayerManipulatingNeuron extends Neuron {
    protected ILayer layer;

    public LayerManipulatingNeuron(Long neuronId, ISignalChain processingChain, Long run, ILayer layer) {
        super(neuronId, processingChain, run);
        this.layer = layer;
    }

    public ILayer getLayer(){
        return layer;
    }
}
