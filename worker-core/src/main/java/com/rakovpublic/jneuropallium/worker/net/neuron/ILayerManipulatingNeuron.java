package com.rakovpublic.jneuropallium.worker.net.neuron;

import com.rakovpublic.jneuropallium.worker.net.layers.ILayer;

/**
 * The neuron which has access to layer
 */
public interface ILayerManipulatingNeuron extends INeuron {
    /**
     * @return the layer object
     */
    ILayer getLayer();
}
