/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.List;

public class IntProcessor implements ISignalProcessor<IntSignal, NeuronIntField> {
    @Override
    public <I extends ISignal> List<I> process(IntSignal input, NeuronIntField neuron) {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public Boolean hasMerger() {
        return null;
    }

    @Override
    public Class<? extends ISignalProcessor> getSignalProcessorClass() {
        return null;
    }

    @Override
    public Class<NeuronIntField> getNeuronClass() {
        return null;
    }
}
