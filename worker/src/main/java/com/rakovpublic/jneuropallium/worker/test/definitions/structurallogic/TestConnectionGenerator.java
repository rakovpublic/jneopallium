/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions.structurallogic;

import com.rakovpublic.jneuropallium.worker.net.neuron.IAxon;
import com.rakovpublic.jneuropallium.worker.net.neuron.IDendrites;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.IWeight;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.Axon;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.Dendrites;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.NeuronAddress;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.NeuronSynapse;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.test.definitions.functionallogic.DummyWeight;
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
                if(neuron.getDendrites()==null){
                    neuron.setDendrites(new Dendrites());
                }
                IDendrites dendrites = neuron.getDendrites();
                for(Integer layerId:layerIds){
                    if(layerId<layerIds.last()){
                        for(INeuron iNeuron: sourceStructure.get(layerId)){
                            for(Class<? extends ISignal> clazz: iNeuron.getResultClasses()){
                                if(neuron.canProcess(clazz)){
                                    dendrites.updateWeight(new NeuronAddress(layerId,iNeuron.getId()),clazz, new DummyWeight());
                                    if(iNeuron.getAxon() ==null){
                                        iNeuron.setAxon(new Axon());
                                    }
                                    iNeuron.getAxon().putConnection(clazz,new NeuronSynapse<>(layerIds.last(), layerId, neuron.getId(), iNeuron.getId(), new DummyWeight<>(), "auto generated"));
                                }
                            }
                        }
                    }
                }
            }
        }
        return sourceStructure;
    }
}
