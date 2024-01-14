/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.List;

public class AProcessor  implements ISignalProcessor<ASignal,NeuronA>{
    @Override
    public <I extends ISignal> List<I> process(ASignal input, NeuronA neuron) {
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
    public Class<NeuronA> getNeuronClass() {
        return null;
    }
}
