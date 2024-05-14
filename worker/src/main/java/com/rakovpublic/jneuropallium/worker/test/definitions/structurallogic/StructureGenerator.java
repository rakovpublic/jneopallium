/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions.structurallogic;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.util.IConnectionGenerator;
import com.rakovpublic.jneuropallium.worker.util.NeighboringRules;
import com.rakovpublic.jneuropallium.worker.util.NeuronNetStructureGenerator;
import com.rakovpublic.jneuropallium.worker.util.NeuronStatisticalProperties;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class StructureGenerator {
    public static void main(String [] args){
        NeuronNetStructureGenerator neuronNetStructureGenerator = new NeuronNetStructureGenerator();
        HashMap<Integer, Long> layerSize = new HashMap<>();
        HashMap<Class<? extends INeuron>, NeuronStatisticalProperties> neuronStatisticalProperties =  new HashMap<>();
        List<NeighboringRules> generationRules = new LinkedList<>();
        generationRules.add(new AnyConfigurationAllowedRule());
        IConnectionGenerator connectionGenerator = new TestConnectionGenerator(generationRules);
        neuronNetStructureGenerator.generateNeuronNet(layerSize, neuronStatisticalProperties, generationRules, connectionGenerator);
    }
}
