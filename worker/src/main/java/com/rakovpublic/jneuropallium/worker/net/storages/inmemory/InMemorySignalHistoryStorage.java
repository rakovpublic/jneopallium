package com.rakovpublic.jneuropallium.worker.net.storages.inmemory;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.ISignalHistoryStorage;
import com.rakovpublic.jneuropallium.worker.net.storages.NeuronAddress;

import java.util.HashMap;
import java.util.List;

public class InMemorySignalHistoryStorage implements ISignalHistoryStorage {
    @Override
    public HashMap<NeuronAddress, List<ISignal>> getSourceSignalsForRun(Long nRun, NeuronAddress forTarget) {
        return null;
    }

    @Override
    public void save(HashMap<Integer, HashMap<Long, List<ISignal>>> history, Long run) {

    }
}
