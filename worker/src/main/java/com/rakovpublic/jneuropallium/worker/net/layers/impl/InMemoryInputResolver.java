package com.rakovpublic.jneuropallium.worker.net.layers.impl;

import com.rakovpublic.jneuropallium.worker.net.layers.IInputResolver;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.*;

import java.util.HashMap;
import java.util.List;

public class InMemoryInputResolver implements IInputResolver {
    private HashMap<IInitInput, InputStatusMeta> inputStatuses;
    private HashMap<IInitInput, InputInitStrategy> inputs;
    private ISignalsPersistStorage signalsPersistStorage;
    private ISignalHistoryStorage signalHistoryStorage;
    private Long currentLoop;
    private IInputLoadingStrategy inputLoadingStrategy;

    public InMemoryInputResolver(ISignalsPersistStorage signalsPersistStorage, ISignalHistoryStorage signalHistoryStorage, IInputLoadingStrategy inputLoadingStrategy) {
        this.signalsPersistStorage = signalsPersistStorage;
        this.signalHistoryStorage = signalHistoryStorage;
        this.inputLoadingStrategy = inputLoadingStrategy;
        currentLoop=0l;
    }

    @Override
    public void registerInput(IInitInput iInputSource, boolean isMandatory, InputInitStrategy initStrategy, Integer amountOfRuns) {
        inputStatuses.put(iInputSource, new InputStatusMeta(true, isMandatory,amountOfRuns, iInputSource.getName()));
        inputs.put(iInputSource,initStrategy);
    }

    @Override
    public ISignalsPersistStorage getSignalPersistStorage() {
        return signalsPersistStorage;
    }

    @Override
    public ISignalHistoryStorage getSignalsHistoryStorage() {
        return signalHistoryStorage;
    }

    @Override
    public HashMap<String, Long> getCycleNeuronAddressMapping() {
        return inputLoadingStrategy.getNeuronInputMapping();
    }

    @Override
    public Integer getCurrentLoop() {
        return inputLoadingStrategy.getCurrentLoopCount();
    }

    @Override
    public void saveHistory() {


    }

    @Override
    public void populateInput() {

    }

    @Override
    public HashMap<String, List<IResultSignal>> getDesiredResult() {
        HashMap<String, List<IResultSignal>> result = new HashMap<>();
        for(IInitInput initInput:inputStatuses.keySet()){
            if(inputStatuses.get(initInput).isBeenUsed()){
                for(String name: initInput.getDesiredResults().keySet()){
                    if(result.get(name)!=null){
                        result.get(name).addAll(initInput.getDesiredResults().get(name));
                    }else{
                        result.put(name,initInput.getDesiredResults().get(name));
                    }
                }
            }
        }
        return result;
    }

}
