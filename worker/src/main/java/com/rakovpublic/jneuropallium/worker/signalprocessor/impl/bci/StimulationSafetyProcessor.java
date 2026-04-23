/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.bci;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci.IStimulationSafetyGateNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.StimulationCommandSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: every {@link StimulationCommandSignal} passes
 * through an {@link IStimulationSafetyGateNeuron}. If the gate returns
 * a non-null veto string the command is dropped; otherwise the command
 * is forwarded untouched for downstream actuator dispatch.
 */
public class StimulationSafetyProcessor implements ISignalProcessor<StimulationCommandSignal, IStimulationSafetyGateNeuron> {

    private static final String DESCRIPTION = "Stimulation safety-envelope gate";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(StimulationCommandSignal input, IStimulationSafetyGateNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        String veto = neuron.veto(input, input.getEpoch());
        if (veto == null) out.add((I) input);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return StimulationSafetyProcessor.class; }
    @Override public Class<IStimulationSafetyGateNeuron> getNeuronClass() { return IStimulationSafetyGateNeuron.class; }
    @Override public Class<StimulationCommandSignal> getSignalClass() { return StimulationCommandSignal.class; }
}
