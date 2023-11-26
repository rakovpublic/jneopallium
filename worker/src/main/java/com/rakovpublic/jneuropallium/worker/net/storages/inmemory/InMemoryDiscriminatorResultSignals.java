/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.storages.inmemory;


import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.storages.INeuronNetInput;
import com.rakovpublic.jneuropallium.worker.net.storages.InMemoryInitInput;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class InMemoryDiscriminatorResultSignals implements INeuronNetInput {
    private InMemoryInitInput callback;
    private String name;
    private List<IInputSignal> inputSignals;
    private ResultLayerHolder resultLayer;

    public InMemoryDiscriminatorResultSignals(InMemoryInitInput callback, String name, ResultLayerHolder resultSignals) {
        this.callback = callback;
        this.name = name;
        this.inputSignals = new LinkedList<>();
        this.resultLayer = resultSignals;

    }

    @Override
    public List<IInputSignal> readSignals() {
        List<IResult> results = resultLayer.getResultLayer().interpretResult();
        for (IResult resultSignal : results) {
            inputSignals.add(new ResultInputSignalWrapper(resultSignal.getResult()));
        }
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
    public void sendCallBack(List<IInputSignal> signals) {
        callback.putSignals(signals);
    }
}
