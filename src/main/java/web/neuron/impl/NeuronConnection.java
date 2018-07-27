package web.neuron.impl;

import web.neuron.INConnection;
import web.neuron.IWeight;

public class NeuronConnection implements INConnection {
    private int targetLayerId;
    private int sourceLayerId;
    private Long targetNeuronId;
    private Long sourceNeuronId;
    private IWeight weight;
    private String description;
    public static  NeuronConnection createConnection(int targetLayerId, int sourceLayerId, Long targetNeuronId, Long sourceNeuronId, IWeight weight, String description){
        return new NeuronConnection( targetLayerId,  sourceLayerId,  targetNeuronId,  sourceNeuronId,  weight,  description);
    }

    private NeuronConnection(int targetLayerId, int sourceLayerId, Long targetNeuronId, Long sourceNeuronId, IWeight weight, String description) {
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
