package com.rakovpublic.jneuropallium.worker.net.layers;

import java.util.List;
import java.util.Objects;

public class LayerMove {
    private Integer layerRemoved;
    private Integer nextLayer;

    private List<Long> nextLayerNeuronIds;

    public LayerMove(Integer layerRemoved, Integer nextLayer, List<Long> nextLayerNeuronIds) {
        this.layerRemoved = layerRemoved;
        this.nextLayer = nextLayer;
        this.nextLayerNeuronIds = nextLayerNeuronIds;
    }

    public Integer getLayerRemoved() {
        return layerRemoved;
    }

    public void setLayerRemoved(Integer layerRemoved) {
        this.layerRemoved = layerRemoved;
    }

    public Integer getNextLayer() {
        return nextLayer;
    }

    public void setNextLayer(Integer nextLayer) {
        this.nextLayer = nextLayer;
    }

    public List<Long> getNextLayerNeuronIds() {
        return nextLayerNeuronIds;
    }

    public void setNextLayerNeuronIds(List<Long> nextLayerNeuronIds) {
        this.nextLayerNeuronIds = nextLayerNeuronIds;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LayerMove layerMove = (LayerMove) o;
        return Objects.equals(layerRemoved, layerMove.layerRemoved) && Objects.equals(nextLayer, layerMove.nextLayer) && Objects.equals(nextLayerNeuronIds, layerMove.nextLayerNeuronIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(layerRemoved, nextLayer, nextLayerNeuronIds);
    }
}
