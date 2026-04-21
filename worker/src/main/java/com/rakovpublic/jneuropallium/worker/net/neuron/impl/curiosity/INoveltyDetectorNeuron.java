package com.rakovpublic.jneuropallium.worker.net.neuron.impl.curiosity;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.curiosity.NoveltySignal;

public interface INoveltyDetectorNeuron extends IModulatableNeuron {
    NoveltySignal evaluate(String contextHash);
    long getTick();
    int getBitCount();
    int getDecayTicks();
}
