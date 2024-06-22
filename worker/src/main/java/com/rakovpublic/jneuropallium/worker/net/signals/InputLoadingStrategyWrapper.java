/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.signals;

import com.rakovpublic.jneuropallium.worker.net.layers.ILayersMeta;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;

import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public class InputLoadingStrategyWrapper implements IInputLoadingStrategy {
    private final IInputLoadingStrategy iInputLoadingStrategy;
    private final Class<? extends IInputLoadingStrategy> clazz;

    public InputLoadingStrategyWrapper(IInputLoadingStrategy iInputLoadingStrategy) {
        this.iInputLoadingStrategy = iInputLoadingStrategy;
        clazz = iInputLoadingStrategy.getClass();
    }

    @Override
    public Boolean populateInput(ISignalsPersistStorage signalsPersistStorage, HashMap<IInitInput, InputStatusMeta> inputStatuses) {
        return iInputLoadingStrategy.populateInput(signalsPersistStorage, inputStatuses);
    }

    @Override
    public void setLayersMeta(ILayersMeta layersMeta) {
        iInputLoadingStrategy.setLayersMeta(layersMeta);
    }

    @Override
    public HashMap<String, Long> getNeuronInputMapping() {
        return iInputLoadingStrategy.getNeuronInputMapping();
    }

    @Override
    public Integer getCurrentLoopCount() {
        return iInputLoadingStrategy.getCurrentLoopCount();
    }

    @Override
    public Long getEpoch() {
        return iInputLoadingStrategy.getEpoch();
    }

    @Override
    public void updateInputs(HashMap<IInitInput, InputStatusMeta> inputStatuses, HashMap<IInitInput, InputInitStrategy> inputs) {
        iInputLoadingStrategy.updateInputs(inputStatuses, inputs);
    }

    @Override
    public void registerInput(IInitInput initInput, InputInitStrategy initStrategy) {
        iInputLoadingStrategy.registerInput(initInput, initStrategy);
    }

    @Override
    public TreeMap<Long, TreeMap<Integer, List<IInputSignal>>> getInputHistory() {
        return iInputLoadingStrategy.getInputHistory();
    }

}
