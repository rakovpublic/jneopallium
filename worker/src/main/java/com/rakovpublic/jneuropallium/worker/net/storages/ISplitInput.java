package com.rakovpublic.jneuropallium.worker.net.storages;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.HashMap;
import java.util.List;

public interface ISplitInput extends IStructMeta {
    HashMap<Long, List<ISignal>> readInputs();
    List<ISignal> readInputsForNeuron(Long neuronId);
    void saveResults(HashMap<Long, List<ISignal>> signals);
}
