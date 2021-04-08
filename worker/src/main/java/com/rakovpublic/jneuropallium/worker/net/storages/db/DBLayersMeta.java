package com.rakovpublic.jneuropallium.worker.net.storages.db;

import com.rakovpublic.jneuropallium.worker.net.storages.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.net.storages.ILayersMeta;
import com.rakovpublic.jneuropallium.worker.net.storages.IResultLayerMeta;

import java.util.List;

public class DBLayersMeta implements ILayersMeta {
    @Override
    public List<ILayerMeta> getLayers() {
        return null;
    }

    @Override
    public IResultLayerMeta getResultLayer() {
        return null;
    }

    @Override
    public ILayerMeta getLayerByID(int id) {
        return null;
    }
}
