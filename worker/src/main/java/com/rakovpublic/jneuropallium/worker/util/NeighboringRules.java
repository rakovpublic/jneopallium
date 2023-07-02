package com.rakovpublic.jneuropallium.worker.util;

import com.rakovpublic.jneuropallium.worker.neuron.INeuron;

import java.util.HashMap;
import java.util.List;

public interface NeighboringRules {
    boolean canBeNeighbours(INeuron candidate, HashMap<Integer, List<INeuron>>  existedNeurons);
}
