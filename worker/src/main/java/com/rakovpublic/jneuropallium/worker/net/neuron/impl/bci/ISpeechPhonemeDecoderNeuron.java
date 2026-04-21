package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;

public interface ISpeechPhonemeDecoderNeuron extends IModulatableNeuron {
    void trainPhoneme(String symbol, double[] centroid);
    int vocabSize();
    String classify(double[] feature);
    String getLastPhoneme();
    double getLastConfidence();
}
