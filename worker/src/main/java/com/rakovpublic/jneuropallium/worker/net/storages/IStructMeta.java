package com.rakovpublic.jneuropallium.worker.net.storages;

import com.rakovpublic.jneuropallium.worker.net.layers.IInputResolver;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.study.IStudyingRequest;

import java.util.HashMap;
import java.util.List;

public interface IStructMeta extends IStorageMeta {
    List<ILayerMeta> getLayers();

    IInputResolver getInputResolver();


    void study(List<IStudyingRequest> requests);

    IResultLayerMeta getResultLayer();

}
