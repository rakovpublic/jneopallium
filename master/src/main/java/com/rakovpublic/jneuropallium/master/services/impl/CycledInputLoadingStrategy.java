package com.rakovpublic.jneuropallium.master.services.impl;

import com.rakovpublic.jneuropallium.master.services.IInputLoadingStrategy;
import com.rakovpublic.jneuropallium.master.services.ISignalsPersistStorage;
import com.rakovpublic.jneuropallium.worker.net.storages.IInitInput;
import com.rakovpublic.jneuropallium.worker.net.storages.ILayersMeta;
import com.rakovpublic.jneuropallium.worker.net.storages.InputInitStrategy;

import java.util.HashMap;

public class CycledInputLoadingStrategy implements IInputLoadingStrategy {
    private ILayersMeta layersMeta;
    private HashMap<IInitInput,InputInitStrategy> externalInputs;

    public CycledInputLoadingStrategy(ILayersMeta layersMeta, HashMap<IInitInput, InputInitStrategy> externalInputs) {
        this.layersMeta = layersMeta;
        this.externalInputs = externalInputs;
    }

    private void init(){


    }

    @Override
    public Boolean populateInput(ISignalsPersistStorage signalsPersistStorage, HashMap<IInitInput, InputStatusMeta> inputStatuses) {
        return null;
    }
}
