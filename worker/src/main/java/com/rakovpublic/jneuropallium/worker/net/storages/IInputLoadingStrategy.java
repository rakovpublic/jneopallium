package com.rakovpublic.jneuropallium.worker.net.storages;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.HashMap;

//interface for cycle running before next run with next input
@JsonDeserialize(using = InputLoadingStrategyDeserializer.class)
public interface IInputLoadingStrategy {
    Boolean populateInput(ISignalsPersistStorage signalsPersistStorage, HashMap<IInitInput, InputStatusMeta> inputStatuses);

    void setLayersMeta(ILayersMeta layersMeta);

    HashMap<String, Long> getNeuronInputMapping();

    Integer getCurrentLoopCount();

    void updateInputs(HashMap<IInitInput, InputStatusMeta> inputStatuses, HashMap<IInitInput, InputInitStrategy> inputs);
}
