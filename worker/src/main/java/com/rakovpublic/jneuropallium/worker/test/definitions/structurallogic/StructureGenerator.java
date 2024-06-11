/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions.structurallogic;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.net.layers.impl.file.FileLayerMeta;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.test.definitions.functionallogic.*;
import com.rakovpublic.jneuropallium.worker.util.IConnectionGenerator;
import com.rakovpublic.jneuropallium.worker.util.NeighboringRules;
import com.rakovpublic.jneuropallium.worker.util.NeuronNetStructureGenerator;
import com.rakovpublic.jneuropallium.worker.util.NeuronStatisticalProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class StructureGenerator {
    private static final Logger logger = LogManager.getLogger(StructureGenerator.class);

    public static void main(String [] args){
        NeuronNetStructureGenerator neuronNetStructureGenerator = new NeuronNetStructureGenerator();
        HashMap<Integer, Long> layerSize = new HashMap<>();
        layerSize.put(0,100l);
        layerSize.put(1,100l);
        layerSize.put(2,100l);
        layerSize.put(3,20l);
        HashMap<Class<? extends INeuron>, NeuronStatisticalProperties> neuronStatisticalProperties =  new HashMap<>();
        HashMap<Integer, Float> probabilityA = new HashMap<>();
        HashMap<ISignalProcessor, Float> processorProbabilityMapA = new HashMap<>();
        probabilityA.put(0,0f);
        probabilityA.put(1,0f);
        probabilityA.put(2,0f);
        probabilityA.put(3,1f);
        processorProbabilityMapA.put( new IntProcessor(),1f);
        processorProbabilityMapA.put(new DoubleProcessor(),1f);
        NeuronAStatisticalProperties neuronAStatisticalProperties = new NeuronAStatisticalProperties(probabilityA, processorProbabilityMapA);
        neuronStatisticalProperties.put(NeuronA.class,neuronAStatisticalProperties);

        HashMap<Integer, Float> probabilityB = new HashMap<>();
        HashMap< ISignalProcessor, Float> processorProbabilityMapB = new HashMap<>();
        probabilityB.put(0,0.5f);
        probabilityB.put(1,0.5f);
        probabilityB.put(2,0.5f);
        probabilityB.put(3,0f);
        processorProbabilityMapB.put(new IntProcessor(),1f);
        NeuronBStatisticalProperties neuronBStatisticalProperties = new NeuronBStatisticalProperties(probabilityB, processorProbabilityMapB);
        neuronStatisticalProperties.put(NeuronB.class,neuronBStatisticalProperties);

        HashMap<Integer, Float> probabilityC = new HashMap<>();
        HashMap< ISignalProcessor, Float> processorProbabilityMapC = new HashMap<>();
        probabilityC.put(0,0.5f);
        probabilityC.put(1,0.5f);
        probabilityC.put(2,0.5f);
        probabilityC.put(3,0f);
        processorProbabilityMapC.put(new DoubleProcessor(),1f);
        NeuronCStatisticalProperties neuronCStatisticalProperties = new NeuronCStatisticalProperties(probabilityC, processorProbabilityMapC);
        neuronStatisticalProperties.put(NeuronC.class,neuronCStatisticalProperties);


        List<NeighboringRules> generationRules = new LinkedList<>();
        generationRules.add(new AnyConfigurationAllowedRule());
        IConnectionGenerator connectionGenerator = new TestConnectionGenerator(generationRules);
        HashMap<Integer,List<INeuron>> layersMap = neuronNetStructureGenerator.generateNeuronNet(layerSize, neuronStatisticalProperties, generationRules, connectionGenerator);
        for(Integer layerId : layersMap.keySet()){
            StringBuilder sb = new StringBuilder();
            sb.append("{\"layerID\":\"");
            sb.append(layerId + "\",");
            sb.append("\"layerSize\":\"");
            sb.append(layersMap.get(layerId).size() + "\",");
            sb.append("\"neurons\":");
            ObjectMapper mapper = new ObjectMapper();
            mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
            String serializedObject = null;
            try {
                serializedObject = mapper.writeValueAsString(layersMap.get(layerId));
            } catch (JsonProcessingException e) {
                logger.error("cannot save  neurons to json ", e);
                e.printStackTrace();
            }
            sb.append(serializedObject);
            sb.append(",\"metaParams\":");
            String serializedMetaParams = "{}";
            sb.append(serializedMetaParams);
            sb.append("}");
            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new FileWriter("F:\\git\\"+layerId));
                writer.write(sb.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }finally {

                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


        }
    }
}
