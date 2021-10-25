package com.rakovpublic.jneuropallium.worker.net.storages;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.HashMap;

/**
 * This interface represents logic of populating signals from inputs
 *
 * */
@JsonDeserialize(using = InputLoadingStrategyDeserializer.class)
public interface IInputLoadingStrategy {
    /**
     * @param inputStatuses map with input and metadata
     * @param signalsPersistStorage storage for layers input
     * @return true if populated successful
     * */
    Boolean populateInput(ISignalsPersistStorage signalsPersistStorage, HashMap<IInitInput, InputStatusMeta> inputStatuses);

    /**
     * @param layersMeta layers metadata object
     * */
    void setLayersMeta(ILayersMeta layersMeta);

    /**
     *
     * @return service neuron mapping on Long.Min layerId for example for additional control neurons it has mapping between initinput name and neuron id
     * */
    HashMap<String, Long> getNeuronInputMapping();
    /**
     * @return current amount of loops after previous input population
     * */
    Integer getCurrentLoopCount();

    /**
     * This method updates inputs and mapping
     * @param inputs mapping between initinputs and input init strategies
     * @param inputStatuses mapping between initinputs and input status metadata
     * */
    void updateInputs(HashMap<IInitInput, InputStatusMeta> inputStatuses, HashMap<IInitInput, InputInitStrategy> inputs);
}
