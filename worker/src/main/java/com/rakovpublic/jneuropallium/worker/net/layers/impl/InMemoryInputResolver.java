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
    private HashMap<Long,HashMap<Integer, HashMap<Long, List<ISignal>>>> signalsHistory;
    @Override
    public void registerInput(IInitInput iInputSource, boolean isMandatory, InputInitStrategy initStrategy, Integer amountOfRuns) {

    }

    @Override
    public ISignalsPersistStorage getSignalPersistStorage() {
        return null;
    }

    @Override
    public ISignalHistoryStorage getSignalsHistoryStorage() {
        return null;
    }

    @Override
    public HashMap<String, Long> getCycleNeuronAddressMapping() {
        return null;
    }

    @Override
    public Long getCurrentLoop() {
        return null;
    }

    @Override
    public void addForHistory(HashMap<Integer, HashMap<Long, List<ISignal>>> history) {

    }

    @Override
    public void populateInput() {

    }

    @Override
    public IResultSignal getDesiredResult() {
        return null;
    }
}
