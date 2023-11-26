/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.storages.inmemory;


import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.storages.INeuronNetInput;
import com.rakovpublic.jneuropallium.worker.net.storages.InMemoryInitInput;
import com.rakovpublic.jneuropallium.worker.neuron.IResultNeuron;
import com.rakovpublic.jneuropallium.worker.neuron.impl.cycleprocessing.ProcessingFrequency;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class InMemoryDiscriminatorResultSignals implements INeuronNetInput {
    private InMemoryInitInput callback;
    private String name;
    private List<IInputSignal> inputSignals;
    private ResultLayerHolder resultLayer;
    private ProcessingFrequency processingFrequency;

    public InMemoryDiscriminatorResultSignals(InMemoryInitInput callback, String name, ResultLayerHolder resultSignals, ProcessingFrequency processingFrequency) {
        this.callback = callback;
        this.name = name;
        this.processingFrequency = processingFrequency;
        this.inputSignals = new LinkedList<>();
        this.resultLayer = resultSignals;

    }

    @Override
    public List<IInputSignal> readSignals() {
        inputSignals.clear();
        if (resultLayer.getResultLayer() != null) {
            List<IResult> results = resultLayer.getResultLayer().interpretResult();
            for (IResult resultSignal : results) {
                inputSignals.add(new ResultInputSignalWrapper(resultSignal.getResult()));
            }
        } else {
            for (IResultNeuron resultNeuron : resultLayer.getResultLayerMeta().getResultNeurons()) {
                inputSignals.add(new ResultInputSignalWrapper(resultNeuron.getFinalResult()));
            }
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
    public ProcessingFrequency getDefaultProcessingFrequency() {
        return processingFrequency;
    }

    @Override
    public void sendCallBack(List<IInputSignal> signals) {
        callback.putSignals(signals);
    }
}
