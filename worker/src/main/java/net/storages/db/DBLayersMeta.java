package net.storages.db;

import net.storages.ILayerMeta;
import net.storages.ILayersMeta;
import net.storages.IResultLayerMeta;

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
