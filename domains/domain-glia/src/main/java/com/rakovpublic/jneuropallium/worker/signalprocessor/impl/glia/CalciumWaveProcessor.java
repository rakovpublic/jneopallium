/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.glia;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.glia.IAstrocyteNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.glia.CalciumWaveSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: the amplitude of an arriving
 * {@link CalciumWaveSignal} is accumulated into the target
 * {@link IAstrocyteNeuron}. The neuron may then emit its own wave, which
 * this processor forwards onward.
 */
public class CalciumWaveProcessor implements ISignalProcessor<CalciumWaveSignal, IAstrocyteNeuron> {

    private static final String DESCRIPTION = "Astrocyte calcium-wave integration";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(CalciumWaveSignal input, IAstrocyteNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        neuron.accumulate(input.getAmplitude());
        CalciumWaveSignal emitted = neuron.maybeEmitWave();
        if (emitted != null) out.add((I) emitted);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return CalciumWaveProcessor.class; }
    @Override public Class<IAstrocyteNeuron> getNeuronClass() { return IAstrocyteNeuron.class; }
    @Override public Class<CalciumWaveSignal> getSignalClass() { return CalciumWaveSignal.class; }
}
