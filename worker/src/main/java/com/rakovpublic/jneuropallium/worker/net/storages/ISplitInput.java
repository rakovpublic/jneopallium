package com.rakovpublic.jneuropallium.worker.net.storages;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.neuron.INeuron;

import java.util.HashMap;
import java.util.List;

public interface ISplitInput extends IStorageMeta {
    HashMap<Long, List<ISignal>> readInputs();
    List<ISignal> readInputsForNeuron(Long neuronId);
    void saveResults(HashMap<Long, List<ISignal>> signals);
    void setNodeIdentifier(String name);
    ISplitInput getNewInstance();
    List<? extends INeuron> getNeurons();
}
