/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.IModeControllerNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.InterlockSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: any interlock trip forces the plant-mode state
 * machine into EMERGENCY.
 */
public class InterlockModeProcessor implements ISignalProcessor<InterlockSignal, IModeControllerNeuron> {

    private static final String DESCRIPTION = "Interlock-driven plant-mode transition";

    @Override
    public <I extends ISignal> List<I> process(InterlockSignal input, IModeControllerNeuron neuron) {
        if (input != null && neuron != null) neuron.onInterlock(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return InterlockModeProcessor.class; }
    @Override public Class<IModeControllerNeuron> getNeuronClass() { return IModeControllerNeuron.class; }
    @Override public Class<InterlockSignal> getSignalClass() { return InterlockSignal.class; }
}
