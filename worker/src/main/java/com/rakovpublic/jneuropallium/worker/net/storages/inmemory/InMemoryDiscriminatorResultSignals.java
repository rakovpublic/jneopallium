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
import com.rakovpublic.jneuropallium.worker.net.storages.InMemoryInitInput;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class InMemoryDiscriminatorResultSignals implements INeuronNetInput {
    private InMemoryInitInput callback;
    private String name;
    private List <IInputSignal> inputSignals;

    public InMemoryDiscriminatorResultSignals(InMemoryInitInput callback, String name, List<IResultSignal> resultSignals) {
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
    public HashMap<String, List<IResultSignal>> getDesiredResults() {
        return new HashMap<>();
    }

    @Override
    public void sendCallBack(List<ISignal> signals) {
        callback.putSignals(signals);
    }
}
