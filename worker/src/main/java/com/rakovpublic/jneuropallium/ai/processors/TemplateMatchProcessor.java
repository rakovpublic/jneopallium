package com.rakovpublic.jneuropallium.ai.processors;

import com.rakovpublic.jneuropallium.ai.neurons.features.IFeatureDetectorNeuron;
import com.rakovpublic.jneuropallium.ai.signals.fast.SpikeSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.List;

public class TemplateMatchProcessor implements ISignalProcessor<SpikeSignal, IFeatureDetectorNeuron> {

    @Override
    public <I extends ISignal> List<I> process(SpikeSignal input, IFeatureDetectorNeuron neuron) {
        List<I> results = new ArrayList<>();
        double[] template = neuron.getWeightedTemplate();
        if (template == null || template.length == 0) return results;
        // Dot product: treat input magnitude as scalar contribution
        double dotProduct = 0.0;
        for (double w : template) dotProduct += w * input.getMagnitude();
        if (dotProduct > neuron.getThreshold()) {
            SpikeSignal spike = new SpikeSignal(true, dotProduct, 1);
            spike.setSourceNeuronId(neuron.getId());
            results.add((I) spike);
        }
        return results;
    }

    @Override public String getDescription() { return "TemplateMatchProcessor"; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return TemplateMatchProcessor.class; }
    @Override public Class<IFeatureDetectorNeuron> getNeuronClass() { return IFeatureDetectorNeuron.class; }
    @Override public Class<SpikeSignal> getSignalClass() { return SpikeSignal.class; }
}
