package com.rakovpublic.jneuropallium.master.services;

import com.rakovpublic.jneuropallium.worker.neuron.INeuron;

public interface ILayerService extends Service {
    void deleteNeuron(Integer layerId, Long neuronId);

    <N extends INeuron> void addNeuron(N neuronJson, Integer layerId);

    void isProcessed(Long layerId);

}
