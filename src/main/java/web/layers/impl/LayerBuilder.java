package web.layers.impl;

import web.storages.IInputMeta;
import web.storages.ILayersMeta;

//TODO:add implementation
public class LayerBuilder {
    private ILayersMeta layersMeta;

    public LayerBuilder(ILayersMeta layersMeta) {
        this.layersMeta = layersMeta;
    }

    public LayerBuilder withLayerId(int layerId){
        return this;
    }
    public LayerBuilder withInput(IInputMeta meta){
        return this;
    }
    public LayerBuilder withNeuronRange(int start, int end){
        return  this;
    }

}
