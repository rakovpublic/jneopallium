package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.Neuron;

public class RecognitionLearningNeuron extends Neuron implements IRecognitionLearningNeuron {
    private final ConvolutionalRecognitionNetwork network;

    public RecognitionLearningNeuron() {
        this(new ConvolutionalRecognitionNetwork());
    }

    public RecognitionLearningNeuron(ConvolutionalRecognitionNetwork network) {
        super();
        this.network = network == null ? new ConvolutionalRecognitionNetwork() : network;
        this.currentNeuronClass = RecognitionLearningNeuron.class;
        this.resultClasses.add(RecognitionLearningResultSignal.class);
        addSignalProcessor(RecognitionFeedbackSignal.class, new RecognitionLearningProcessor());
    }

    @Override
    public ConvolutionalRecognitionNetwork getNetwork() {
        return network;
    }

    public RecognitionLearningResultSignal learn(RecognitionFeedbackSignal feedback) {
        return applyFeedback(feedback);
    }

    @Override
    public RecognitionLearningResultSignal applyFeedback(RecognitionFeedbackSignal feedback) {
        RecognitionLearningResultSignal result = network.applyFeedback(feedback);
        setChanged(result.getUpdatedMatrices() > 0);
        return result;
    }
}
