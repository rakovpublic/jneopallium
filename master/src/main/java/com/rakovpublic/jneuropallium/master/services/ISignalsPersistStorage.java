package com.rakovpublic.jneuropallium.master.services;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.HashMap;
import java.util.List;

public interface ISignalsPersistStorage {
    void putSignals(HashMap<Integer, HashMap<Long, List<ISignal>>> signals);

    HashMap<Long, List<ISignal>> getLayerSignals(Integer layerId);

    void cleanOutdatedSignals();
}
