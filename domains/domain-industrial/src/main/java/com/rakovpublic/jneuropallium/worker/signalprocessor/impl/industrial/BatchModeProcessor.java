/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.IModeControllerNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.BatchStateSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: informs the plant-mode state machine of batch
 * phase transitions (ISA-88).
 */
public class BatchModeProcessor implements ISignalProcessor<BatchStateSignal, IModeControllerNeuron> {

    private static final String DESCRIPTION = "Batch-phase-driven plant-mode transition";

    @Override
    public <I extends ISignal> List<I> process(BatchStateSignal input, IModeControllerNeuron neuron) {
        if (input != null && neuron != null) neuron.onBatchState(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return BatchModeProcessor.class; }
    @Override public Class<IModeControllerNeuron> getNeuronClass() { return IModeControllerNeuron.class; }
    @Override public Class<BatchStateSignal> getSignalClass() { return BatchStateSignal.class; }
}
