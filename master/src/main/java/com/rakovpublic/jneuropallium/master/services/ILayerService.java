package com.rakovpublic.jneuropallium.master.services;

import com.rakovpublic.jneuropallium.master.model.INeuronLayer;
import com.rakovpublic.jneuropallium.master.model.ISignalLayer;
import com.rakovpublic.jneuropallium.worker.neuron.INeuron;

import java.util.List;

public interface ILayerService {
    void deleteNeuron(Integer layerId, Long neuronId);

    <N extends INeuron> void  addNeuron(N neuronJson, Integer layerId);

    void isProcessed(Long layerId);

}
