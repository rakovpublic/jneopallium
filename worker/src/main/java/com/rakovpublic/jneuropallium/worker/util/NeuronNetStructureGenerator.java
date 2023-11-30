package com.rakovpublic.jneuropallium.worker.util;


import com.rakovpublic.jneuropallium.worker.net.layers.ILayersMeta;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class NeuronNetStructureGenerator {

    /**
     * register neuron in the layer
     *
     * @param layerSize                   contains information of size for each level
     * @param neuronStatisticalProperties contains information of statical properties of neuron i.e probability that it exists on the layer and probability that it has specific processors
     * @param generationRules             rules which allows to understand if two neurons can have connection
     */
    public HashMap<Integer,List<INeuron>> generateNeuronNet(HashMap<Integer, Long> layerSize,
                                         HashMap<Class<? extends INeuron>, NeuronStatisticalProperties> neuronStatisticalProperties,
                                         List<NeighboringRules> generationRules, IConnectionGenerator connectionGenerator) {
        ILayersMeta layersMeta = null;
        Random random = new Random();
        HashMap<Integer, List<INeuron>> layersSource = new HashMap<>();
        for (Integer layerId : layerSize.keySet()) {
            Long neuronCount = 0l;
            Long safeFlag = Long.MIN_VALUE;
            for (; neuronCount < layerSize.get(layerId) && safeFlag < layerSize.get(layerId); ) {
                for (Class<? extends INeuron> clazz : neuronStatisticalProperties.keySet()) {
                    if (random.nextFloat() < neuronStatisticalProperties.get(clazz).getProbability(layerId)) {
                        INeuron neuron = neuronStatisticalProperties.get(clazz).getNeuronInstance(Long.MIN_VALUE + 10l + neuronCount, layerId);
                        boolean toContinue = true;
                        for (NeighboringRules rule : generationRules) {
                            toContinue = rule.canBeNeighbours(neuron, layersSource);
                            if (!toContinue) {
                                break;
                            }
                        }
                        if (layersSource.containsKey(layerId) && toContinue) {
                            layersSource.get(layerId).add(neuron);
                            neuronCount++;
                            break;
                        } else if (toContinue) {
                            ArrayList<INeuron> neurons = new ArrayList<>();
                            neurons.add(neuron);
                            layersSource.put(layerId, neurons);
                            neuronCount++;
                            break;
                        }
                    }
                }
                safeFlag++;
            }
        }
        layersSource = connectionGenerator.generateConnections(layersSource);
        return layersSource;
    }

}
