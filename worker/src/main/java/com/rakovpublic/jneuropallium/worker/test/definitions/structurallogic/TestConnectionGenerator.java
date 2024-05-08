/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions.structurallogic;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.util.IConnectionGenerator;
import com.rakovpublic.jneuropallium.worker.util.NeighboringRules;

import java.util.HashMap;
import java.util.List;

public class TestConnectionGenerator implements IConnectionGenerator {
    private List<NeighboringRules> generationRules;

    public TestConnectionGenerator(List<NeighboringRules> generationRules) {
        this.generationRules = generationRules;
    }

    @Override
    public HashMap<Integer, List<INeuron>> generateConnections(HashMap<Integer, List<INeuron>> sourceStructure) {
        return null;
    }
}
