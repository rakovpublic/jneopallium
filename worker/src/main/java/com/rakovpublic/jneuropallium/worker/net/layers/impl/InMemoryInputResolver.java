package com.rakovpublic.jneuropallium.worker.net.layers.impl;

import com.rakovpublic.jneuropallium.worker.net.signals.*;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInputResolver;
import com.rakovpublic.jneuropallium.worker.net.signals.INeuronNetInput;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

public class InMemoryInputResolver implements IInputResolver {
    private HashMap<IInitInput, InputStatusMeta> inputStatuses;
    private HashMap<IInitInput, InputInitStrategy> inputs;
    private ISignalsPersistStorage signalsPersistStorage;
    private ISignalHistoryStorage signalHistoryStorage;
    private Long epoch;
    private IInputLoadingStrategy inputLoadingStrategy;
    private HashMap<String, IInitInput> initInput;

    public InMemoryInputResolver(ISignalsPersistStorage signalsPersistStorage, ISignalHistoryStorage signalHistoryStorage, IInputLoadingStrategy inputLoadingStrategy) {
        this.signalsPersistStorage = signalsPersistStorage;
        this.signalHistoryStorage = signalHistoryStorage;
        this.inputLoadingStrategy = inputLoadingStrategy;
        epoch = 0l;
        inputStatuses = new HashMap<>();
        inputs = new HashMap<>();
        initInput = new HashMap<>();
    }

    @Override
    public void registerInput(IInitInput iInputSource, boolean isMandatory, InputInitStrategy initStrategy) {
        inputStatuses.put(iInputSource, new InputStatusMeta(true, isMandatory, iInputSource.getName()));
        inputs.put(iInputSource, initStrategy);
        inputLoadingStrategy.updateInputs(inputStatuses, inputs);
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
    public TreeMap<Long, TreeMap<Integer, List<IInputSignal>>> getInputHistory() {
        return inputLoadingStrategy.getInputHistory();
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
    public Long getRun() {
        return epoch;
    }

    @Override
    public void saveHistory() {
        signalHistoryStorage.save(signalsPersistStorage.getAllSignals(), epoch, inputLoadingStrategy.getCurrentLoopCount());
    }

    @Override
    public void populateInput() {
        inputLoadingStrategy.populateInput(signalsPersistStorage, inputStatuses);
    }

    @Override
    public HashMap<String, List<IResultSignal>> getDesiredResult() {
        HashMap<String, List<IResultSignal>> result = new HashMap<>();
        for (IInitInput initInput : inputStatuses.keySet()) {
            if (inputStatuses.get(initInput).isBeenUsed()) {
                for (String name : initInput.getDesiredResults().keySet()) {
                    if (result.get(name) != null) {
                        result.get(name).addAll(initInput.getDesiredResults().get(name));
                    } else {
                        result.put(name, initInput.getDesiredResults().get(name));
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void sendCallBack(String name, List<ISignal> signals) {
        IInitInput input = initInput.get(name);
        if (input instanceof INeuronNetInput) {
            List<IInputSignal> inputSignals = new LinkedList<>();
            for (ISignal signal : signals) {
                inputSignals.add(new SimpleInputSignalWrapper(signal));
            }
            ((INeuronNetInput) input).sendCallBack(inputSignals);
        }
    }

}
