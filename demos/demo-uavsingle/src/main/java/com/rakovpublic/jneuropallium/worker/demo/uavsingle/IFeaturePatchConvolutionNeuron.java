package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;

public interface IFeaturePatchConvolutionNeuron extends INeuron {
    ConvolutionFeatureSignal fire(FeaturePatchSignal patch);
}
