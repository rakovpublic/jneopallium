package com.rakovpublic.jneuropallium.worker.neuron.impl;

import com.rakovpublic.jneuropallium.worker.neuron.INConnection;
import com.rakovpublic.jneuropallium.worker.neuron.IWeight;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class NeuronConnection<K extends ISignal> implements INConnection<K> {
    private int targetLayerId;
    private int sourceLayerId;
    private Long targetNeuronId;
    private Long sourceNeuronId;
    private IWeight weight;
    private String description;

    public NeuronConnection() {
    }
    public NeuronConnection(NeuronConnection connection, IWeight weight) {
        this.targetLayerId = connection.targetLayerId;
        this.sourceLayerId = connection.sourceLayerId;
        this.targetNeuronId = connection.targetNeuronId;
        this.sourceNeuronId = connection.sourceNeuronId;
        this.weight = new WeightWrapper(connection.weight);
        this.description = connection.description;
    }

    public static NeuronConnection createConnection(int targetLayerId, int sourceLayerId, Long targetNeuronId, Long sourceNeuronId, IWeight weight, String description) {
        return new NeuronConnection(targetLayerId, sourceLayerId, targetNeuronId, sourceNeuronId, weight, description);
    }

    public NeuronConnection(int targetLayerId, int sourceLayerId, Long targetNeuronId, Long sourceNeuronId, IWeight weight, String description) {
        this.targetLayerId = targetLayerId;
        this.sourceLayerId = sourceLayerId;
        this.targetNeuronId = targetNeuronId;
        this.sourceNeuronId = sourceNeuronId;
        this.weight = weight;
        this.description = description;
    }

    @Override
    public int getTargetLayerId() {
        return targetLayerId;
    }

    @Override
    public int getSourceLayerId() {
        return sourceLayerId;
    }

    @Override
    public Long getTargetNeuronId() {
        return targetNeuronId;
    }

    @Override
    public Long getSourceNeuronId() {
        return sourceNeuronId;
    }

    @Override
    public String toJSON() {
        return null;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public IWeight getWeight() {
        return weight;
    }


}
