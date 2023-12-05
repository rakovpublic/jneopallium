/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.signals;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;

import java.util.HashMap;
import java.util.List;

public class InitInputWrapper implements IInitInput {
    private IInitInput initInput;
    private Class<? extends IInitInput> clazz;

    public InitInputWrapper(IInitInput initInput) {
        this.initInput = initInput;
        clazz = initInput.getClass();
    }

    @Override
    public List<IInputSignal> readSignals() {
        return initInput.readSignals();
    }

    @Override
    public String getName() {
        return initInput.getName();
    }


    @Override
    public HashMap<String, List<IResultSignal>> getDesiredResults() {
        return initInput.getDesiredResults();
    }

    @Override
    public ProcessingFrequency getDefaultProcessingFrequency() {
        return initInput.getDefaultProcessingFrequency();
    }
}
