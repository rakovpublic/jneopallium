package com.rakovpublic.jneuropallium.ai.processors;

import com.rakovpublic.jneuropallium.ai.neurons.input.IAdaptiveSensoryNeuron;
import com.rakovpublic.jneuropallium.ai.signals.fast.SensorySignal;
import com.rakovpublic.jneuropallium.ai.signals.fast.SpikeSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.List;

public class AdaptiveThresholdProcessor implements ISignalProcessor<SensorySignal, IAdaptiveSensoryNeuron> {

    @Override
    public <I extends ISignal> List<I> process(SensorySignal input, IAdaptiveSensoryNeuron neuron) {
        List<I> results = new ArrayList<>();
        double value = (input.getRawValues() != null && input.getRawValues().length > 0) ? input.getRawValues()[0] : 0.0;
        // Always decay threshold
        neuron.setThreshold(neuron.getThreshold() * (1.0 - neuron.getDecayRate()));
        if (value > neuron.getThreshold()) {
            neuron.setThreshold(neuron.getThreshold() + neuron.getAdaptationRate());
            SpikeSignal spike = new SpikeSignal(true, value, 1);
            spike.setSourceNeuronId(neuron.getId());
            results.add((I) spike);
        }
        return results;
    }

    @Override public String getDescription() { return "AdaptiveThresholdProcessor"; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return AdaptiveThresholdProcessor.class; }
    @Override public Class<IAdaptiveSensoryNeuron> getNeuronClass() { return IAdaptiveSensoryNeuron.class; }
    @Override public Class<SensorySignal> getSignalClass() { return SensorySignal.class; }
}
