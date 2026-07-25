package com.rakovpublic.jneuropallium.ai.processors;

import com.rakovpublic.jneuropallium.ai.neurons.input.ISensoryEncoderNeuron;
import com.rakovpublic.jneuropallium.ai.signals.fast.SensorySignal;
import com.rakovpublic.jneuropallium.ai.signals.fast.SpikeSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.List;

public class PopulationCodeProcessor implements ISignalProcessor<SensorySignal, ISensoryEncoderNeuron> {

    @Override
    public <I extends ISignal> List<I> process(SensorySignal input, ISensoryEncoderNeuron neuron) {
        List<I> results = new ArrayList<>();
        if (input.getRawValues() == null || input.getRawValues().length == 0) return results;
        double inputVal = input.getRawValues()[0];
        double preferred = neuron.getPreferredValue();
        double sigma = neuron.getSigma();
        double diff = (inputVal - preferred) / (sigma > 0 ? sigma : 1.0);
        double magnitude = Math.exp(-0.5 * diff * diff);
        boolean fired = magnitude > 0.1;
        SpikeSignal spike = new SpikeSignal(fired, magnitude, 1);
        spike.setSourceLayerId(input.getSourceLayerId());
        spike.setSourceNeuronId(neuron.getId());
        results.add((I) spike);
        return results;
    }

    @Override public String getDescription() { return "PopulationCodeProcessor"; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return PopulationCodeProcessor.class; }
    @Override public Class<ISensoryEncoderNeuron> getNeuronClass() { return ISensoryEncoderNeuron.class; }
    @Override public Class<SensorySignal> getSignalClass() { return SensorySignal.class; }
}
