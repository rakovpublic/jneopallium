package com.rakovpublic.jneuropallium.worker.net.storages.structimpl;

import com.rakovpublic.jneuropallium.worker.net.layers.IInputResolver;
import com.rakovpublic.jneuropallium.worker.net.storages.ILayersMeta;

public class StructBuilder {
    private IInputResolver hiddenInputMeta;
    private ILayersMeta layersMeta;

    public StructBuilder() {

    }

    public StructBuilder(IInputResolver hiddenInputMeta, ILayersMeta layersMeta) {
        this.hiddenInputMeta = hiddenInputMeta;
        this.layersMeta = layersMeta;
    }

    public StructBuilder withHiddenInputMeta(IInputResolver hiddenInputMeta) {
        this.hiddenInputMeta = hiddenInputMeta;
        return this;
    }

    public StructBuilder withLayersMeta(ILayersMeta layersMeta) {
        this.layersMeta = layersMeta;
        return this;
    }


    public StructMeta build() {
        if (hiddenInputMeta != null && layersMeta != null) {
            return new StructMeta(hiddenInputMeta, layersMeta);
        }
        return null;
    }
}
