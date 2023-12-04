/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.layers.impl.redis;

import com.rakovpublic.jneuropallium.worker.net.layers.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayersMeta;
import com.rakovpublic.jneuropallium.worker.net.layers.IResultLayerMeta;

import java.util.List;
//TODO: add implementation
public class RedisLayersMeta implements ILayersMeta {
    @Override
    public void setRootPath(String path) {

    }

    @Override
    public List<ILayerMeta> getLayers() {
        return null;
    }

    @Override
    public IResultLayerMeta getResultLayer() {
        return null;
    }

    @Override
    public ILayerMeta getLayerByPosition(int id) {
        return null;
    }

    @Override
    public void addLayerMeta(ILayerMeta layerMeta) {

    }

    @Override
    public void addLayerMeta(ILayerMeta layerMeta, int position) {

    }

    @Override
    public void removeLayer(ILayerMeta layerMeta) {

    }

    @Override
    public ILayerMeta getLayerById(int id) {
        return null;
    }
}
