/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions.servicelogic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.test.definitions.functionallogic.DoubleSignal;
import com.rakovpublic.jneuropallium.worker.test.definitions.functionallogic.IntSignal;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class ProcessingFrequencyMapGenerator {
    public static void main(String [] args){
        HashMap<String, ProcessingFrequency> processingFrequencyHashMap = new HashMap<>();
        processingFrequencyHashMap.put(DoubleSignal.class.getCanonicalName(), new ProcessingFrequency(1l,1));
        processingFrequencyHashMap.put(IntSignal.class.getCanonicalName(), new ProcessingFrequency(1l,1));
        ObjectMapper mapper = new ObjectMapper();
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter("F:\\git\\processingfrequencymap"));
            writer.write(mapper.writeValueAsString(processingFrequencyHashMap));
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
