/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.signals;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayersMeta;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;

import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

/**
 * This interface represents logic of populating signals from inputs
 */
@JsonDeserialize(using = InputLoadingStrategyDeserializer.class)
public interface IInputLoadingStrategy {
    /**
     * @param inputStatuses         map with input and metadata
     * @param signalsPersistStorage storage for layers input
     * @return true if populated successful
     */
    Boolean populateInput(ISignalsPersistStorage signalsPersistStorage, HashMap<IInitInput, InputStatusMeta> inputStatuses);

    /**
     * @param layersMeta layers metadata object
     */
    void setLayersMeta(ILayersMeta layersMeta);

    /**
     * @return service neuron mapping on Long.Min layerId for example for additional control neurons it has mapping between initinput name and neuron id
     */
    HashMap<String, Long> getNeuronInputMapping();

    /**
     * @return current amount of loops after previous input population
     */
    Integer getCurrentLoopCount();


    /**
     * @return current amount of run after start excluding loops
     */
    Long getEpoch();

    /**
     * This method updates inputs and mapping
     *
     * @param inputs        mapping between initinputs and input init strategies
     * @param inputStatuses mapping between initinputs and input status metadata
     */
    void updateInputs(HashMap<IInitInput, InputStatusMeta> inputStatuses, HashMap<IInitInput, InputInitStrategy> inputs);

    void registerInput(IInitInput initInput, InputInitStrategy initStrategy);


    TreeMap<Long, TreeMap<Integer, List<IInputSignal>>> getInputHistory();
}
