package com.rakovpublic.jneuropallium.worker.net.neuron.impl.layersizing;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class DeleteNeuronIntegration {
    private Long neuronId;
    private HashMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> createRelationsSignals;

    public Long getNeuronId() {
        return neuronId;
    }

    public void setNeuronId(Long neuronId) {
        this.neuronId = neuronId;
    }

    public HashMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> getCreateRelationsSignals() {
        return createRelationsSignals;
    }

    public void setCreateRelationsSignals(HashMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> createRelationsSignals) {
        this.createRelationsSignals = createRelationsSignals;
    }
}
