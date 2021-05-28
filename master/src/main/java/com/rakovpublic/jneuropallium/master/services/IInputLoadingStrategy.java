package com.rakovpublic.jneuropallium.master.services;

import com.rakovpublic.jneuropallium.worker.net.storages.InputStatusMeta;
import com.rakovpublic.jneuropallium.worker.net.storages.IInitInput;
import com.rakovpublic.jneuropallium.worker.net.storages.ILayersMeta;

import java.util.HashMap;

//interface for cycle running before next run with next input
public interface IInputLoadingStrategy {
    Boolean populateInput(ISignalsPersistStorage signalsPersistStorage, HashMap<IInitInput, InputStatusMeta> inputStatuses);
    void setLayersMeta(ILayersMeta layersMeta);
}
