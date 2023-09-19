package com.rakovpublic.jneuropallium.master.services.impl;

import com.rakovpublic.jneuropallium.worker.net.storages.ISplitInput;

public class NodeMeta {
    private Integer currentLayer;
    private Boolean status;
    private ISplitInput  currentInput;
    private Long timestamp;

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


    public ISplitInput getCurrentInput() {
        return currentInput;
    }

    public void setCurrentInput(ISplitInput currentInput) {
        this.currentInput = currentInput;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
