package net.layers.impl;

import net.layers.ILayer;
import net.storages.IInputMeta;
import net.storages.ILayerMeta;
import net.storages.ILayersMeta;

//TODO:add implementation
public class LayerBuilder {
    private ILayerMeta layerMeta;
    private IInputMeta meta;
    private int layerId;

    public LayerBuilder() {

    }

    public LayerBuilder withLayer(ILayerMeta layerMeta){
        this.layerMeta=layerMeta;
        return this;
    }
    public LayerBuilder withInput(IInputMeta meta){
        this.meta=meta;
        return this;
    }
    public LayerBuilder withNeuronRange(int start, int end){
        return  this;
    }

    public ILayer build(){
        //TODO: finish this method
        return null;
    }

}
