package com.rakovpublic.jneuropallium.worker.util;

import com.rakovpublic.jneuropallium.worker.neuron.INeuron;

public interface NeighboringRules {
    boolean canBeNeighbours(INeuron candidate,INeuron existedNeurons);
}
