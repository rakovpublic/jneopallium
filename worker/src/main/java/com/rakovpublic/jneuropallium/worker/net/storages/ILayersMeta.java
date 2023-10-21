package com.rakovpublic.jneuropallium.worker.net.storages;

import java.io.Serializable;
import java.util.List;

/**
 * This interface represents layers storage
 */
public interface ILayersMeta extends Serializable {


    void setRootPath(String path);

    /**
     * @return the list of all layer metas
     */
    List<ILayerMeta> getLayers();

    /**
     * @return the result layer meta
     */
    IResultLayerMeta getResultLayer();

    /**
     * @param id the layer id
     * @return layer id meta
     */
    ILayerMeta getLayerByID(int id);

    /**
     * this method add new layer to current net
     *
     * @param layerMeta
     */
    void addLayerMeta(ILayerMeta layerMeta);

    void removeLayer(ILayerMeta layerMeta);

}
