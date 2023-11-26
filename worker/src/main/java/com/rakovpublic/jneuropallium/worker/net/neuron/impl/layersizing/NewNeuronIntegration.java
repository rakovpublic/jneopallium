package com.rakovpublic.jneuropallium.worker.net.neuron.impl.layersizing;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;

import java.util.HashMap;
import java.util.List;

public class NewNeuronIntegration<K extends INeuron> {
    private HashMap<Integer, HashMap<Long, List<ISignal>>> createRelationsSignals;
    private K neuron;

    public HashMap<Integer, HashMap<Long, List<ISignal>>> getCreateRelationsSignals() {
        return createRelationsSignals;
    }

    public void setCreateRelationsSignals(HashMap<Integer, HashMap<Long, List<ISignal>>> createRelationsSignals) {
        this.createRelationsSignals = createRelationsSignals;
    }

    public K getNeuron() {
        return neuron;
    }

    public void setNeuron(K neuron) {
        this.neuron = neuron;
    }
}
