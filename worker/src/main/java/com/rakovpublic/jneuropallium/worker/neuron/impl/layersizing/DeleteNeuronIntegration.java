package com.rakovpublic.jneuropallium.worker.neuron.impl.layersizing;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.HashMap;
import java.util.List;

public class DeleteNeuronIntegration {
    private Long neuronId;
    private HashMap<Integer, HashMap<Long, List<ISignal>>> createRelationsSignals;

    public Long getNeuronId() {
        return neuronId;
    }

    public void setNeuronId(Long neuronId) {
        this.neuronId = neuronId;
    }

    public HashMap<Integer, HashMap<Long, List<ISignal>>> getCreateRelationsSignals() {
        return createRelationsSignals;
    }

    public void setCreateRelationsSignals(HashMap<Integer, HashMap<Long, List<ISignal>>> createRelationsSignals) {
        this.createRelationsSignals = createRelationsSignals;
    }
}
