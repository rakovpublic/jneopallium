/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.signals.storage.redis;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignalsPersistStorage;

import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

//TODO: add implementation
public class RedisSignalStorage implements ISignalsPersistStorage {
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
    public void cleanMiddleLayerSignals() {

    }

    @Override
    public TreeMap<Integer, HashMap<Long, List<ISignal>>> getAllSignals() {
        return null;
    }

    @Override
    public void deletedLayerInput(Integer deletedLayerId) {

    }
}
