/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.layers;

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
    ILayerMeta getLayerByPosition(int id);


    /**
     * this method add new layer to current net
     *
     * @param layerMeta
     */
    void addLayerMeta(ILayerMeta layerMeta);

    void addLayerMeta(ILayerMeta layerMeta, int position);

    void removeLayer(ILayerMeta layerMeta);

}
