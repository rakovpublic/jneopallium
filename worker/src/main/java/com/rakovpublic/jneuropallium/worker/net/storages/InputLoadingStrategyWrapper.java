package com.rakovpublic.jneuropallium.worker.net.storages;

import java.util.HashMap;

public class InputLoadingStrategyWrapper  implements IInputLoadingStrategy{
    private IInputLoadingStrategy iInputLoadingStrategy;
    private Class<? extends IInputLoadingStrategy> clazz;

    public InputLoadingStrategyWrapper(IInputLoadingStrategy iInputLoadingStrategy) {
        this.iInputLoadingStrategy = iInputLoadingStrategy;
        clazz = iInputLoadingStrategy.getClass();
    }

    @Override
    public Boolean populateInput(ISignalsPersistStorage signalsPersistStorage, HashMap<IInitInput, InputStatusMeta> inputStatuses) {
        return iInputLoadingStrategy.populateInput(signalsPersistStorage,inputStatuses);
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
    public void updateInputs(HashMap<IInitInput, InputStatusMeta> inputStatuses, HashMap<IInitInput, InputInitStrategy> inputs) {
        iInputLoadingStrategy.updateInputs(inputStatuses,inputs);
    }

}
