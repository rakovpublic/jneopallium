package web.neuron.impl;

import web.neuron.INConnection;
import web.neuron.IWeight;

public class NeuronConnection implements INConnection {
    private int targetLayerId;
    private int sourceLayerId;
    private Long targetNeuronId;
    private Long sourceLongId;
    private IWeight weight;
    private String description;

    public NeuronConnection(int targetLayerId, int sourceLayerId, Long targetNeuronId, Long sourceLongId, IWeight weight, String description) {
        this.targetLayerId = targetLayerId;
        this.sourceLayerId = sourceLayerId;
        this.targetNeuronId = targetNeuronId;
        this.sourceLongId = sourceLongId;
        this.weight = weight;
        this.description = description;
    }

    @Override
    public int getTargetLayerId() {
        return 0;
    }

    @Override
    public int getSourceLayerId() {
        return 0;
    }

    @Override
    public Long getTargetNeuronId() {
        return null;
    }

    @Override
    public Long getSourceNeuronId() {
        return null;
    }

    @Override
    public String toJSON() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public IWeight getWeight() {
        return null;
    }


}
