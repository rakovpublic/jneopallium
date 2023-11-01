/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.storages.inmemory;

import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.IInitInput;
import com.rakovpublic.jneuropallium.worker.net.storages.INeuronNetInput;

import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

//TODO: implement
public class InMemoryDiscriminatorSourceSignals implements IInitInput {
    private TreeMap<Long,HashMap<Integer, IInputSignal>> history;
    private Long amountOfEpoch;
    private Integer amountOfLoops;

    public InMemoryDiscriminatorSourceSignals(TreeMap<Long, HashMap<Integer, IInputSignal>> history, Long amountOfEpoch, Integer amountOfLoops) {
        this.history = history;
        this.amountOfEpoch = amountOfEpoch;
        this.amountOfLoops = amountOfLoops;
    }

    @Override
    public List<IInputSignal> readSignals() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public INeuronNetInput getNeuronNetInput() {
        return null;
    }

    @Override
    public HashMap<String, List<IResultSignal>> getDesiredResults() {
        return null;
    }
}
