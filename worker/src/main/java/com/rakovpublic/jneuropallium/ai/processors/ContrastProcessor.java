package com.rakovpublic.jneuropallium.ai.processors;

import com.rakovpublic.jneuropallium.ai.neurons.features.ContrastEnhancerNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.features.IContrastEnhancerNeuron;
import com.rakovpublic.jneuropallium.ai.signals.fast.SpikeSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.List;

public class ContrastProcessor implements ISignalProcessor<SpikeSignal, IContrastEnhancerNeuron> {

    @Override
    public <I extends ISignal> List<I> process(SpikeSignal input, IContrastEnhancerNeuron neuron) {
        List<I> results = new ArrayList<>();
        double enhanced = input.getMagnitude() * neuron.getExcitatory() - neuron.getInhibitory();
        if (enhanced > 0) {
            SpikeSignal spike = new SpikeSignal(true, enhanced, input.getBurstCount());
            spike.setSourceNeuronId(neuron.getId());
            results.add((I) spike);
        }
        return results;
    }

    @Override public String getDescription() { return "ContrastProcessor"; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return ContrastProcessor.class; }
    @Override public Class<IContrastEnhancerNeuron> getNeuronClass() { return IContrastEnhancerNeuron.class; }
    @Override public Class<SpikeSignal> getSignalClass() { return SpikeSignal.class; }
}
