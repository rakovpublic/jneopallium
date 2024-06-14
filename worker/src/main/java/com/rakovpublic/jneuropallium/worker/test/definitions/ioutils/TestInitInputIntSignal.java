/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions.ioutils;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;
import com.rakovpublic.jneuropallium.worker.test.definitions.functionallogic.InputIntSignal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TestInitInputIntSignal implements IInitInput {
    public List<InputIntSignal> inputIntSignals;

    public TestInitInputIntSignal() {
        inputIntSignals = new ArrayList<>();
    }

    public List<InputIntSignal> getInputIntSignals() {
        return inputIntSignals;
    }

    public void setInputIntSignals(List<InputIntSignal> inputIntSignals) {
        this.inputIntSignals = inputIntSignals;
    }
    @Override
    public List<IInputSignal> readSignals() {
        List<IInputSignal> result = new ArrayList<>();
        result.addAll(inputIntSignals);
        inputIntSignals.clear();
        return result;
    }

    @Override
    public String getName() {
        return "Int signals input source";
    }

    @Override
    public HashMap<String, List<IResultSignal>> getDesiredResults() {
        return new HashMap<>();
    }

    @Override
    public ProcessingFrequency getDefaultProcessingFrequency() {
        return new ProcessingFrequency(1l,1);
    }
}