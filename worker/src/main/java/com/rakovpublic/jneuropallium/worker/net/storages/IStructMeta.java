package com.rakovpublic.jneuropallium.worker.net.storages;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.study.IStudyingRequest;

import java.util.HashMap;
import java.util.List;

public interface IStructMeta extends IStorageMeta {
    List<ILayerMeta> getLayers();

    IInputMeta getInputs(int layerId);

    void saveResults(int layerId, HashMap<String, List<ISignal>> meta);

    void study(List<IStudyingRequest> requests);

    IResultLayerMeta getResultLayer();

}
