/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.signals.storage;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignalHistoryStorage;
import com.rakovpublic.jneuropallium.worker.net.neuron.NeuronAddress;

import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public class InMemorySignalHistoryStorage implements ISignalHistoryStorage {
    private TreeMap<Integer, TreeMap<Long, TreeMap<Integer, HashMap<Long, List<ISignal>>>>> history;
    private Integer loopsToStore = 0;
    private Long runsToStore = 0l;

    public InMemorySignalHistoryStorage(Integer loopsToStore, Long runsToStore) {
        this.loopsToStore = loopsToStore;
        this.runsToStore = runsToStore;
        this.history = new TreeMap<>();
    }


    @Override
    public List<ISignal> getSourceSignalsForRun(Integer loop, Long nRun, NeuronAddress forTarget) {
        if (history.containsKey(loop) && history.get(loop).containsKey(nRun)) {
            return history.get(loop).get(nRun).get(forTarget.getLayerId()).get(forTarget.getNeuronId());
        }
        return null;

    }

    @Override
    public void save(TreeMap<Integer, HashMap<Long, List<ISignal>>> history, Long run, Integer loop) {
        if (this.history.size() >= loopsToStore) {
            this.history.remove(history.firstKey());
        }
        if (this.history.containsKey(loop) && this.history.get(loop).size() >= runsToStore) {
            this.history.get(loop).remove(this.history.get(loop).firstKey());
        }
        if (this.history.containsKey(loop)) {
            this.history.get(loop).put(run, history);
        } else {
            TreeMap<Long, TreeMap<Integer, HashMap<Long, List<ISignal>>>> loopHistory = new TreeMap<>();
            loopHistory.put(run, history);
            this.history.put(loop, loopHistory);
        }
    }
}
