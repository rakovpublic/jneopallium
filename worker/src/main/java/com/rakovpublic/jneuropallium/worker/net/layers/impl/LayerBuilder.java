package com.rakovpublic.jneuropallium.worker.net.layers.impl;

import com.rakovpublic.jneuropallium.worker.net.layers.IInputResolver;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayer;
import com.rakovpublic.jneuropallium.worker.net.layers.IResultLayer;
import com.rakovpublic.jneuropallium.worker.net.storages.ILayerMeta;


public class LayerBuilder {
    private ILayerMeta layerMeta;
    private IInputResolver meta;

    public LayerBuilder() {

    }

    public LayerBuilder withLayer(ILayerMeta layerMeta) {
        this.layerMeta = layerMeta;
        return this;
    }

    public LayerBuilder withInput(IInputResolver meta) {
        this.meta = meta;
        return this;
    }

    public LayerBuilder withNeuronRange(int start, int end) {
        //TODO:add implementation
        return this;
    }

    public ILayer build() {
        ILayer layer = new Layer(layerMeta.getID(), meta);
        layer.registerAll(layerMeta.getNeurons());
        return layer;
    }

    public IResultLayer buildResultLayer() {
        IResultLayer layer = new ResultLayer(layerMeta.getID(), meta);
        layer.registerAll(layerMeta.getNeurons());
        return layer;
    }

}
