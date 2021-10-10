package com.rakovpublic.jneuropallium.worker.neuron;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.HashMap;
import java.util.List;

public interface ISignalStorageCallBack {
    List<ISignal> getSignals(Integer layerId, Long neuronId);

    void saveResults(HashMap<Integer, HashMap<Long, List<ISignal>>> result);
}
