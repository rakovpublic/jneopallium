/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.master.services.impl;

import com.rakovpublic.jneuropallium.worker.net.storages.structimpl.StructMeta;

public class DiscriminatorStatus {
    private StructMeta structMeta;
    private String name;
    private boolean processed;
    private boolean valid;
    private int currentLayer;

    public DiscriminatorStatus(StructMeta structMeta, String name, boolean processed, boolean valid, int currentLayer) {
        this.structMeta = structMeta;
        this.name = name;
        this.processed = processed;
        this.valid = valid;
        this.currentLayer = currentLayer;
    }

    public int getCurrentLayer() {
        return currentLayer;
    }

    public void setCurrentLayer(int currentLayer) {
        this.currentLayer = currentLayer;
    }

    public StructMeta getStructMeta() {
        return structMeta;
    }

    public void setStructMeta(StructMeta structMeta) {
        this.structMeta = structMeta;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }
}
