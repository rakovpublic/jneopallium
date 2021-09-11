package com.rakovpublic.jneuropallium.worker.neuron;

import com.rakovpublic.jneuropallium.worker.net.layers.ILayer;

public interface ILayerManipulatingNeuron extends INeuron {
    public ILayer getLayer();
}
