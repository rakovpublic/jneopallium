/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.model;

import com.rakovpublic.jneuropallium.worker.net.layers.LayerMetaParam;

public class LayerParamUpdate {
    private String paramName;
    private Integer layerId;
    private LayerMetaParam layerMetaParam;

    public LayerParamUpdate(String paramName, Integer layerId, LayerMetaParam layerMetaParam) {
        this.paramName = paramName;
        this.layerId = layerId;
        this.layerMetaParam = layerMetaParam;
    }

    public String getParamName() {
        return paramName;
    }

    public void setParamName(String paramName) {
        this.paramName = paramName;
    }

    public LayerMetaParam getLayerMetaParam() {
        return layerMetaParam;
    }

    public void setLayerMetaParam(LayerMetaParam layerMetaParam) {
        this.layerMetaParam = layerMetaParam;
    }

    public Integer getLayerId() {
        return layerId;
    }

    public void setLayerId(Integer layerId) {
        this.layerId = layerId;
    }
}
