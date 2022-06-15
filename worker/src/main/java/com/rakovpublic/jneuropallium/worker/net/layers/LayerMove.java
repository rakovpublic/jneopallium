package com.rakovpublic.jneuropallium.worker.net.layers;

import java.util.Objects;

public class LayerMove {
    private Integer layerRemoved;
    private Integer nextLayer;

    private Integer prevLayer;

    public LayerMove(Integer layerRemoved, Integer nextLayer, Integer prevLayer) {
        this.layerRemoved = layerRemoved;
        this.nextLayer = nextLayer;
        this.prevLayer = prevLayer;
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

    public Integer getPrevLayer() {
        return prevLayer;
    }

    public void setPrevLayer(Integer prevLayer) {
        this.prevLayer = prevLayer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LayerMove layerMove = (LayerMove) o;
        return Objects.equals(layerRemoved, layerMove.layerRemoved) && Objects.equals(nextLayer, layerMove.nextLayer) && Objects.equals(prevLayer, layerMove.prevLayer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(layerRemoved, nextLayer, prevLayer);
    }
}
