package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;

public interface IRecognitionLearningNeuron extends INeuron {
    ConvolutionalRecognitionNetwork getNetwork();

    RecognitionLearningResultSignal applyFeedback(RecognitionFeedbackSignal feedback);
}
