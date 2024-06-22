/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.signals;

import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class represents storage for signals to process
 */
public interface ISignalsPersistStorage {
    /**
     * This method put signals
     *
     * @param signals
     */
    void putSignals(HashMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> signals);

    /**
     * This method extract signals for layer
     *
     * @param layerId
     * @return neuronId signals structure
     */
    HashMap<Long, CopyOnWriteArrayList<ISignal>> getLayerSignals(Integer layerId);

    /**
     * This method cleaned outdated signals
     */
    void cleanOutdatedSignals();

    void cleanMiddleLayerSignals();

    /**
     * This method return signals structure
     *
     * @return layerId neuronId signals structure
     */
    TreeMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> getAllSignals();

    void deletedLayerInput(Integer deletedLayerId);


    boolean hasSignalsToProcess();
}
