/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.layers.impl;

import com.rakovpublic.jneuropallium.worker.net.layers.IResultLayer;
import com.rakovpublic.jneuropallium.worker.net.layers.IResultLayerMeta;

public class ResultLayerHolder {
    private IResultLayer resultLayer;
    private IResultLayerMeta iResultLayerMeta;

    public IResultLayerMeta getResultLayerMeta() {
        return iResultLayerMeta;
    }

    public void setResultLayerMeta(IResultLayerMeta iResultLayerMeta) {
        this.iResultLayerMeta = iResultLayerMeta;
    }

    public ResultLayerHolder() {
    }

    public IResultLayer getResultLayer() {
        return resultLayer;
    }

    public void setResultLayer(IResultLayer resultLayer) {
        this.resultLayer = resultLayer;
    }
}
