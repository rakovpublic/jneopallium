package com.rakovpublic.jneuropallium.worker.net.neuron.impl;

import java.util.Objects;

public class NeuronAddress {
    private Integer layerId;
    private Long neuronId;

    public NeuronAddress(Integer layerId, Long neuronId) {
        this.layerId = layerId;
        this.neuronId = neuronId;
    }

    public NeuronAddress() {
    }

    public Integer getLayerId() {
        return layerId;
    }

    public void setLayerId(Integer layerId) {
        this.layerId = layerId;
    }

    public Long getNeuronId() {
        return neuronId;
    }

    public void setNeuronId(Long neuronId) {
        this.neuronId = neuronId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NeuronAddress that = (NeuronAddress) o;
        return Objects.equals(layerId, that.layerId) &&
                Objects.equals(neuronId, that.neuronId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(layerId, neuronId);
    }
}
