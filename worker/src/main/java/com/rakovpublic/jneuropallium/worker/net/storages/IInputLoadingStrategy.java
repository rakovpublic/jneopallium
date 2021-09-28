package com.rakovpublic.jneuropallium.worker.net.storages;

import com.rakovpublic.jneuropallium.worker.net.storages.ISignalsPersistStorage;
import com.rakovpublic.jneuropallium.worker.net.storages.InputStatusMeta;
import com.rakovpublic.jneuropallium.worker.net.storages.IInitInput;
import com.rakovpublic.jneuropallium.worker.net.storages.ILayersMeta;
import com.rakovpublic.jneuropallium.worker.neuron.INeuron;

import java.util.HashMap;

//interface for cycle running before next run with next input
public interface IInputLoadingStrategy {
    Boolean populateInput(ISignalsPersistStorage signalsPersistStorage, HashMap<IInitInput, InputStatusMeta> inputStatuses);
    void setLayersMeta(ILayersMeta layersMeta);
    HashMap<String,Long> getNeuronInputMapping();
    Integer getCurrentLoopCount();
    void updateInputs(HashMap<IInitInput, InputStatusMeta> inputStatuses,HashMap<IInitInput, InputInitStrategy> inputs);
}
