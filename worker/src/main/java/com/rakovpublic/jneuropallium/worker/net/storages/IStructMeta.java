package com.rakovpublic.jneuropallium.worker.net.storages;

import com.rakovpublic.jneuropallium.worker.net.layers.IInputResolver;
import com.rakovpublic.jneuropallium.worker.net.study.ILearningRequest;

import java.util.List;
/**
 * Deprecated
 * */

public interface IStructMeta extends IStorageMeta {
    List<ILayerMeta> getLayers();

    IInputResolver getInputResolver();


    void study(List<ILearningRequest> requests);

    IResultLayerMeta getResultLayer();

}
