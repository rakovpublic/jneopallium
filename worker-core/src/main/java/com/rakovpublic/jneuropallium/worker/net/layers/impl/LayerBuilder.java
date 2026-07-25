package com.rakovpublic.jneuropallium.worker.net.layers.impl;

import com.rakovpublic.jneuropallium.worker.net.layers.ILayer;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.net.layers.IResultLayer;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInputResolver;


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

    public ILayer build(int threads) {
        ILayer layer = new Layer(layerMeta.getID(), meta, threads);
        layer.registerAll(layerMeta.getNeurons());
        layer.setLayerMetaParams(layerMeta.getLayerMetaParams());
        return layer;
    }

    public IResultLayer buildResultLayer(int threads) {
        IResultLayer layer = new ResultLayer(layerMeta.getID(), meta, threads);
        layer.registerAll(layerMeta.getNeurons());
        layer.setLayerMetaParams(layerMeta.getLayerMetaParams());
        return layer;
    }

}
