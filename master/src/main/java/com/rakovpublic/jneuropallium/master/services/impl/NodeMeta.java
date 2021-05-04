package com.rakovpublic.jneuropallium.master.services.impl;

public class NodeMeta {
    private Integer currentLayer;
    private Boolean status;

    public NodeMeta(Integer currentLayer, Boolean status) {
        this.currentLayer = currentLayer;
        this.status = status;
    }

    public Integer getCurrentLayer() {
        return currentLayer;
    }

    public void setCurrentLayer(Integer currentLayer) {
        this.currentLayer = currentLayer;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }
}
