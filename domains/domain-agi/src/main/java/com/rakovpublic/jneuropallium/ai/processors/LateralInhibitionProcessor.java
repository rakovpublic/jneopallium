package com.rakovpublic.jneuropallium.ai.processors;

import com.rakovpublic.jneuropallium.ai.neurons.features.IInhibitoryInterneuron;
import com.rakovpublic.jneuropallium.ai.signals.fast.AttentionGateSignal;
import com.rakovpublic.jneuropallium.ai.signals.fast.SpikeSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.List;

public class LateralInhibitionProcessor implements ISignalProcessor<SpikeSignal, IInhibitoryInterneuron> {

    private static final double INHIBITION_THRESHOLD = 0.3;

    @Override
    public <I extends ISignal> List<I> process(SpikeSignal input, IInhibitoryInterneuron neuron) {
        List<I> results = new ArrayList<>();
        if (input.getMagnitude() > INHIBITION_THRESHOLD) {
            AttentionGateSignal gate = new AttentionGateSignal(0.0, neuron.getLayerId(), true);
            gate.setSourceNeuronId(neuron.getId());
            results.add((I) gate);
        }
        return results;
    }

    @Override public String getDescription() { return "LateralInhibitionProcessor"; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return LateralInhibitionProcessor.class; }
    @Override public Class<IInhibitoryInterneuron> getNeuronClass() { return IInhibitoryInterneuron.class; }
    @Override public Class<SpikeSignal> getSignalClass() { return SpikeSignal.class; }
}
