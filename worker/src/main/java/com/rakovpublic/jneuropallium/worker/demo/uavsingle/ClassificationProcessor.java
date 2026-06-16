package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.List;

public class ClassificationProcessor implements ISignalProcessor<FeatureVectorSignal, ClassificationNeuron> {
    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(FeatureVectorSignal input, ClassificationNeuron neuron) {
        List<I> result = new ArrayList<>();
        result.add((I) neuron.score(input));
        return result;
    }

    @Override public String getDescription() { return "pooled convolution feature classifier"; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return ClassificationProcessor.class; }
    @Override public Class<ClassificationNeuron> getNeuronClass() { return ClassificationNeuron.class; }
    @Override public Class<FeatureVectorSignal> getSignalClass() { return FeatureVectorSignal.class; }
}
