package com.rakovpublic.jneuropallium.master.services;

import com.rakovpublic.jneuropallium.worker.net.storages.ILayersMeta;

//interface for cycle running before next run with next input
public interface IRunningStrategy {
    void populateInput(ISignalsPersistStorage signalsPersistStorage, ILayersMeta layersMeta);
}
