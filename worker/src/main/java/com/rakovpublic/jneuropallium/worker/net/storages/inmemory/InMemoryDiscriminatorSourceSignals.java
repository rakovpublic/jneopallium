/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.storages.inmemory;

import com.rakovpublic.jneuropallium.worker.net.layers.IInputResolver;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.IInitInput;
import com.rakovpublic.jneuropallium.worker.net.storages.INeuronNetInput;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

public class InMemoryDiscriminatorSourceSignals implements IInitInput {
    private TreeMap<Long,TreeMap<Integer, List<IInputSignal>>> history;
    private Long amountOfEpoch;
    private Integer amountOfLoops;
    private IInputResolver inputResolver;

    private String name;

    public InMemoryDiscriminatorSourceSignals(TreeMap<Long, TreeMap<Integer, List<IInputSignal>>> history, Long amountOfEpoch, Integer amountOfLoops, IInputResolver inputResolver, String name) {
        this.history = history;
        this.amountOfEpoch = amountOfEpoch;
        this.amountOfLoops = amountOfLoops;
        this.inputResolver = inputResolver;
        this.name = name;
    }

    @Override
    public List<IInputSignal> readSignals() {
        List<IInputSignal> results = new LinkedList<>();
        for(Long i = inputResolver.getRun()-amountOfEpoch>history.firstKey()?inputResolver.getRun()-amountOfEpoch:history.firstKey(); i<inputResolver.getRun(); i++){
            for(Integer j = Math.max(history.get(i).lastKey() - amountOfLoops, history.get(i).firstKey()); j<history.get(i).lastKey(); j++){
                results.addAll(history.get(i).get(j));
            }
        }
        return results;
    }

    @Override
    public String getName() {
        return name;
    }



    @Override
    public HashMap<String, List<IResultSignal>> getDesiredResults() {
        return new HashMap<>();
    }
}
