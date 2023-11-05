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
    private IInputResolver history;
    private Long amountOfEpoch;
    private Integer amountOfLoops;
    private String name;

    public InMemoryDiscriminatorSourceSignals(IInputResolver history, Long amountOfEpoch, Integer amountOfLoops,  String name) {
        this.history = history;
        this.amountOfEpoch = amountOfEpoch;
        this.amountOfLoops = amountOfLoops;
        this.name = name;
    }

    @Override
    public List<IInputSignal> readSignals() {
        List<IInputSignal> results = new LinkedList<>();
        for(Long i = history.getRun()-amountOfEpoch>history.getInputHistory().firstKey()?history.getRun()-amountOfEpoch:history.getInputHistory().firstKey(); i<history.getRun(); i++){
            for(Integer j = Math.max(history.getInputHistory().get(i).lastKey() - amountOfLoops, history.getInputHistory().get(i).firstKey()); j<history.getInputHistory().get(i).lastKey(); j++){
                results.addAll(history.getInputHistory().get(i).get(j));
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
