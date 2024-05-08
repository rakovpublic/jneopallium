/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions.structurallogic;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.util.NeighboringRules;

import java.util.*;

public class AnyConfigurationAllowedRule implements NeighboringRules {
    @Override
    public boolean canBeNeighbours(INeuron candidate, HashMap<Integer, List<INeuron>> existedNeurons) {
       return true;
    }
}
