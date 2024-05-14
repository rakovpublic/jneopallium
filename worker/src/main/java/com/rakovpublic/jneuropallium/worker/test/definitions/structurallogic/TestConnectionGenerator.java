/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions.structurallogic;

import com.rakovpublic.jneuropallium.worker.net.neuron.IAxon;
import com.rakovpublic.jneuropallium.worker.net.neuron.IDendrites;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.util.IConnectionGenerator;
import com.rakovpublic.jneuropallium.worker.util.NeighboringRules;

import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

public class TestConnectionGenerator implements IConnectionGenerator {
    private List<NeighboringRules> generationRules;

    public TestConnectionGenerator(List<NeighboringRules> generationRules) {
        this.generationRules = generationRules;
    }

    @Override
    public HashMap<Integer, List<INeuron>> generateConnections(HashMap<Integer, List<INeuron>> sourceStructure) {
        TreeSet<Integer> layerIds = new TreeSet<>();
        layerIds.addAll(sourceStructure.keySet());
        if(sourceStructure.size()>1){
            List<INeuron> neuronsToConnect = sourceStructure.get(layerIds.last());
            for(INeuron neuron : neuronsToConnect){
                IDendrites dendrites = neuron.getDendrites();
                for(Integer layerId:layerIds){
                    if(layerId<layerIds.last()){
                        for(INeuron iNeuron: sourceStructure.get(layerId)){
                            //TODO: finish connection generation
                        }
                    }
                }
            }
        }
        return sourceStructure;
    }
}
