package net.layers.impl;

import net.storages.IInputMeta;
import net.storages.ILayersMeta;

//TODO:add implementation
public class LayerBuilder {
    private ILayersMeta layersMeta;
    private IInputMeta meta;
    private int layerId;

    public LayerBuilder(ILayersMeta layersMeta) {
        this.layersMeta = layersMeta;
    }

    public LayerBuilder withLayerId(int layerId){
        this.layerId=layerId;
        return this;
    }
    public LayerBuilder withInput(IInputMeta meta){
        this.meta=meta;
        return this;
    }
    public LayerBuilder withNeuronRange(int start, int end){
        return  this;
    }

}
