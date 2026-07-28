/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.layers;

import com.rakovpublic.jneuropallium.worker.net.signals.ReconnectStrategy;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInputResolver;
import com.rakovpublic.jneuropallium.worker.net.storages.IStorageMeta;
import com.rakovpublic.jneuropallium.worker.net.study.ILearningRequest;

import java.util.List;

/**
 * Deprecated
 */

public interface IStructMeta extends IStorageMeta {
    List<ILayerMeta> getLayers();

    IInputResolver getInputResolver();


    void learn(List<ILearningRequest> requests);

    IResultLayerMeta getResultLayer();

    void removeLayer(Integer layerId, ReconnectStrategy reconnectStrategy);

}
