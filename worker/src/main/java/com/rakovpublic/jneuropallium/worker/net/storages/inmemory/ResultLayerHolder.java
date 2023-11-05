/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.storages.inmemory;

import com.rakovpublic.jneuropallium.worker.net.layers.IResultLayer;

public class ResultLayerHolder {
    private IResultLayer resultLayer;

    public ResultLayerHolder() {
    }

    public IResultLayer getResultLayer() {
        return resultLayer;
    }

    public void setResultLayer(IResultLayer resultLayer) {
        this.resultLayer = resultLayer;
    }
}
