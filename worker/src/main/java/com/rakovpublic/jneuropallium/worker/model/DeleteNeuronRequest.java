package com.rakovpublic.jneuropallium.worker.model;

public class DeleteNeuronRequest {
    private Long neuronId;
    private Integer layerId;

    public Long getNeuronId() {
        return neuronId;
    }

    public void setNeuronId(Long neuronId) {
        this.neuronId = neuronId;
    }

    public Integer getLayerId() {
        return layerId;
    }

    public void setLayerId(Integer layerId) {
        this.layerId = layerId;
    }
}
