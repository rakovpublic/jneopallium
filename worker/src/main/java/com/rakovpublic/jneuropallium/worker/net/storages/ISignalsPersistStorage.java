package com.rakovpublic.jneuropallium.worker.net.storages;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.HashMap;
import java.util.List;

/**
 * This class represents storage for signals to process
 * */
public interface ISignalsPersistStorage {
    /**
     * This method put signals
     * @param signals
     *
     * */
    void putSignals(HashMap<Integer, HashMap<Long, List<ISignal>>> signals);

    /**
     * This method extract signals for layer
     * @param layerId
     * @return neuronId signals structure
     * */
    HashMap<Long, List<ISignal>> getLayerSignals(Integer layerId);

    /**
     * This method cleaned outdated signals
     *
     * */
    void cleanOutdatedSignals();

    /**
     * This method return signals structure
     * @return layerId neuronId signals structure
     * */
    HashMap<Integer, HashMap<Long, List<ISignal>>> getAllSignals();

}
