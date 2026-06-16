package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.List;

public class RecognitionLearningProcessor implements ISignalProcessor<RecognitionFeedbackSignal, IRecognitionLearningNeuron> {
    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(RecognitionFeedbackSignal input, IRecognitionLearningNeuron neuron) {
        List<I> result = new ArrayList<>();
        result.add((I) neuron.applyFeedback(input));
        return result;
    }

    @Override public String getDescription() { return "recognition reinforcement feedback processor"; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return RecognitionLearningProcessor.class; }
    @Override public Class<IRecognitionLearningNeuron> getNeuronClass() { return IRecognitionLearningNeuron.class; }
    @Override public Class<RecognitionFeedbackSignal> getSignalClass() { return RecognitionFeedbackSignal.class; }
}
