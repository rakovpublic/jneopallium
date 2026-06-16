package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;

public interface IFeatureClassificationNeuron extends INeuron {
    ClassificationScoreSignal score(FeatureVectorSignal vector);
}
