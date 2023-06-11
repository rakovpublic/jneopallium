package com.rakovpublic.jneuropallium.worker.util;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.net.storages.ILayersMeta;
import com.rakovpublic.jneuropallium.worker.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.neuron.ISignalProcessor;

import java.util.HashMap;
import java.util.List;

public class NeuronNetStructureGenerator {

    /**
     * register neuron in the layer
     *
     * @param layerSize contains information of size for each level
     * @param neuronStatisticalProperties contains information of statical properties of neuron i.e probability that it exists on the layer and probability that it has specific processors
     * @param  generationRules rules which allows to understand if two neurons can have connection
     */
    public ILayersMeta generateNeuronNet(HashMap<Integer,Long> layerSize,
                                         NeuronStatisticalProperties neuronStatisticalProperties,
                                         List<NeighboringRules> generationRules){
        ILayersMeta layersMeta = null;
        //TODO: add implementation
        return layersMeta;
    }

}
