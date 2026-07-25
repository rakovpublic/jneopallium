package com.rakovpublic.jneuropallium.worker.net.neuron.impl;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISynapse;
import com.rakovpublic.jneuropallium.worker.net.neuron.IWeight;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class NeuronSynapse<K extends ISignal> implements ISynapse<K> {
    private int targetLayerId;
    private int sourceLayerId;
    private Long targetNeuronId;
    private Long sourceNeuronId;
    private IWeight weight;
    private String description;

    public NeuronSynapse() {
    }

    public NeuronSynapse(NeuronSynapse connection, IWeight weight) {
        this.targetLayerId = connection.targetLayerId;
        this.sourceLayerId = connection.sourceLayerId;
        this.targetNeuronId = connection.targetNeuronId;
        this.sourceNeuronId = connection.sourceNeuronId;
        this.weight = new WeightWrapper(connection.weight);
        this.description = connection.description;
    }

    public static NeuronSynapse createConnection(int targetLayerId, int sourceLayerId, Long targetNeuronId, Long sourceNeuronId, IWeight weight, String description) {
        return new NeuronSynapse(targetLayerId, sourceLayerId, targetNeuronId, sourceNeuronId, weight, description);
    }

    public NeuronSynapse(int targetLayerId, int sourceLayerId, Long targetNeuronId, Long sourceNeuronId, IWeight weight, String description) {
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

    @Override
    public void setWeight(IWeight<K, ? extends ISignal> weight) {
        this.weight = weight;
    }


}
