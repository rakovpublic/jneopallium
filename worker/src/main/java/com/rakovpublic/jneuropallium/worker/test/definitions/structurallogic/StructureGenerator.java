/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions.structurallogic;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.test.definitions.functionallogic.*;
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
        layerSize.put(0,100l);
        layerSize.put(1,100l);
        layerSize.put(2,100l);
        layerSize.put(3,20l);
        HashMap<Class<? extends INeuron>, NeuronStatisticalProperties> neuronStatisticalProperties =  new HashMap<>();
        HashMap<Integer, Float> probabilityA = new HashMap<>();
        HashMap<Class<? extends ISignalProcessor>, Float> processorProbabilityMapA = new HashMap<>();
        probabilityA.put(3,1f);
        processorProbabilityMapA.put(IntProcessor.class,1f);
        processorProbabilityMapA.put(DoubleProcessor.class,1f);
        NeuronAStatisticalProperties neuronAStatisticalProperties = new NeuronAStatisticalProperties(probabilityA, processorProbabilityMapA);
        neuronStatisticalProperties.put(NeuronA.class,neuronAStatisticalProperties);

        HashMap<Integer, Float> probabilityB = new HashMap<>();
        HashMap<Class<? extends ISignalProcessor>, Float> processorProbabilityMapB = new HashMap<>();
        probabilityA.put(0,0.5f);
        probabilityA.put(1,0.5f);
        probabilityA.put(2,0.5f);
        processorProbabilityMapB.put(IntProcessor.class,1f);
        NeuronBStatisticalProperties neuronBStatisticalProperties = new NeuronBStatisticalProperties(probabilityB, processorProbabilityMapB);
        neuronStatisticalProperties.put(NeuronB.class,neuronBStatisticalProperties);

        HashMap<Integer, Float> probabilityC = new HashMap<>();
        HashMap<Class<? extends ISignalProcessor>, Float> processorProbabilityMapC = new HashMap<>();
        probabilityA.put(0,0.5f);
        probabilityA.put(1,0.5f);
        probabilityA.put(2,0.5f);
        processorProbabilityMapC.put(DoubleProcessor.class,1f);
        NeuronCStatisticalProperties neuronCStatisticalProperties = new NeuronCStatisticalProperties(probabilityC, processorProbabilityMapC);
        neuronStatisticalProperties.put(NeuronC.class,neuronCStatisticalProperties);


        List<NeighboringRules> generationRules = new LinkedList<>();
        generationRules.add(new AnyConfigurationAllowedRule());
        IConnectionGenerator connectionGenerator = new TestConnectionGenerator(generationRules);
        neuronNetStructureGenerator.generateNeuronNet(layerSize, neuronStatisticalProperties, generationRules, connectionGenerator);
    }
}
