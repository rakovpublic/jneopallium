package com.rakovpublic.jneuropallium.worker.net.storages.inmemory;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.ISignalsPersistStorage;

import java.util.HashMap;
import java.util.List;

public class InMemorySignalPersistStorage implements ISignalsPersistStorage {
    @Override
    public void putSignals(HashMap<Integer, HashMap<Long, List<ISignal>>> signals) {

    }

    @Override
    public HashMap<Long, List<ISignal>> getLayerSignals(Integer layerId) {
        return null;
    }

    @Override
    public void cleanOutdatedSignals() {

    }

    @Override
    public HashMap<Integer, HashMap<Long, List<ISignal>>> getAllSignals() {
        return null;
    }
}
