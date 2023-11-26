/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.signals.storage;

import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputLoadingStrategy;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class InMemoryDiscriminatorSourceSignals implements IInitInput {
    private IInputLoadingStrategy history;
    private Long amountOfEpoch;
    private Integer amountOfLoops;
    private String name;
    private ProcessingFrequency processingFrequency;

    public InMemoryDiscriminatorSourceSignals(IInputLoadingStrategy history, Long amountOfEpoch, Integer amountOfLoops, String name, ProcessingFrequency processingFrequency) {
        this.history = history;
        this.amountOfEpoch = amountOfEpoch;
        this.amountOfLoops = amountOfLoops;
        this.name = name;
        this.processingFrequency = processingFrequency;
    }

    @Override
    public List<IInputSignal> readSignals() {
        List<IInputSignal> results = new LinkedList<>();
        for (Long i = history.getEpoch() - amountOfEpoch > history.getInputHistory().firstKey() ? history.getEpoch() - amountOfEpoch : history.getInputHistory().firstKey(); i < history.getEpoch(); i++) {
            for (Integer j = Math.max(history.getInputHistory().get(i).lastKey() - amountOfLoops, history.getInputHistory().get(i).firstKey()); j < history.getInputHistory().get(i).lastKey(); j++) {
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

    @Override
    public ProcessingFrequency getDefaultProcessingFrequency() {
        return processingFrequency;
    }
}
