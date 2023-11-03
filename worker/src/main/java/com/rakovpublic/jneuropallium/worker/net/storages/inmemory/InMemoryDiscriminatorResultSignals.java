/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.storages.inmemory;

import com.rakovpublic.jneuropallium.worker.net.layers.IInputResolver;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.storages.IInitInput;
import com.rakovpublic.jneuropallium.worker.net.storages.INeuronNetInput;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class InMemoryDiscriminatorResultSignals implements IInitInput {
    private INeuronNetInput callback;
    private String name;
    private List <IInputSignal> inputSignals;

    public InMemoryDiscriminatorResultSignals(INeuronNetInput callback, String name, List<IResultSignal> resultSignals) {
        this.callback = callback;
        this.name = name;
        this.inputSignals = new LinkedList<>();
        for(IResultSignal resultSignal: resultSignals){
            inputSignals.add(new ResultInputSignalWrapper(resultSignal));
        }
    }

    @Override
    public List<IInputSignal> readSignals() {
        return inputSignals;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public INeuronNetInput getNeuronNetInput() {
        return callback;
    }

    @Override
    public HashMap<String, List<IResultSignal>> getDesiredResults() {
        return new HashMap<>();
    }
}
