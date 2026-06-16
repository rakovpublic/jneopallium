package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.List;

public class FeaturePatchConvolutionProcessor implements ISignalProcessor<FeaturePatchSignal, ConvolutionalPerceptronNeuron> {
    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(FeaturePatchSignal input, ConvolutionalPerceptronNeuron neuron) {
        List<I> result = new ArrayList<>();
        result.add((I) neuron.fire(input));
        return result;
    }

    @Override public String getDescription() { return "3x3 feature patch convolution processor"; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return FeaturePatchConvolutionProcessor.class; }
    @Override public Class<ConvolutionalPerceptronNeuron> getNeuronClass() { return ConvolutionalPerceptronNeuron.class; }
    @Override public Class<FeaturePatchSignal> getSignalClass() { return FeaturePatchSignal.class; }
}
