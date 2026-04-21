package com.rakovpublic.jneuropallium.worker.net.neuron.impl.curiosity;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.curiosity.NoveltySignal;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;

public interface INoveltyDetectorNeuron extends IModulatableNeuron {
    NoveltySignal evaluate(String contextHash);
    long getTick();
    int getBitCount();
    int getDecayTicks();
}
