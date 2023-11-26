/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.neuron;

import java.util.Objects;

public class NeuronAddress {
    private Long neuronId;
    private Integer layerId;

    public NeuronAddress(Long neuronId, Integer layerId) {
        this.neuronId = neuronId;
        this.layerId = layerId;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NeuronAddress that = (NeuronAddress) o;
        return Objects.equals(neuronId, that.neuronId) &&
                Objects.equals(layerId, that.layerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(neuronId, layerId);
    }
}
