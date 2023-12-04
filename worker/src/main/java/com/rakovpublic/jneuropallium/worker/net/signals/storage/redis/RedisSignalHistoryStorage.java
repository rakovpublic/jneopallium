/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.signals.storage.redis;

import com.rakovpublic.jneuropallium.worker.net.neuron.NeuronAddress;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignalHistoryStorage;

import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
//TODO: add implementation
public class RedisSignalHistoryStorage  implements ISignalHistoryStorage {
    @Override
    public List<ISignal> getSourceSignalsForRun(Integer loop, Long nRun, NeuronAddress forTarget) {
        return null;
    }

    @Override
    public void save(TreeMap<Integer, HashMap<Long, List<ISignal>>> history, Long run, Integer loop) {

    }
}
