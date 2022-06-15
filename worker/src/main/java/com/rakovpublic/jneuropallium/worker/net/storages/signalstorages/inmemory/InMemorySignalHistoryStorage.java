package com.rakovpublic.jneuropallium.worker.net.storages.signalstorages.inmemory;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.ISignalHistoryStorage;
import com.rakovpublic.jneuropallium.worker.net.storages.NeuronAddress;

import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public class InMemorySignalHistoryStorage implements ISignalHistoryStorage {
    private HashMap<Long, TreeMap<Integer, HashMap<Long, List<ISignal>>>> history;

    public InMemorySignalHistoryStorage() {
        this.history = new HashMap<>();
    }

    @Override
    public List<ISignal> getSourceSignalsForRun(Long nRun, NeuronAddress forTarget) {
        return history.get(nRun).get(forTarget.getLayerId()).get(forTarget.getNeuronId());
    }

    @Override
    public void save(TreeMap<Integer, HashMap<Long, List<ISignal>>> history, Long run) {
        this.history.put(run, history);
    }
}
