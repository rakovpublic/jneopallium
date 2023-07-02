package com.rakovpublic.jneuropallium.worker.util;

import com.rakovpublic.jneuropallium.worker.neuron.INeuron;

import java.util.HashMap;
import java.util.List;

public interface IConnectionGenerator {
    HashMap<Integer, List<INeuron>> generateConnections(HashMap<Integer,List<INeuron>> sourceStructure );
}
