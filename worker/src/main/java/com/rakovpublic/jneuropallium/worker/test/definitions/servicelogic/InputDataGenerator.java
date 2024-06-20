/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions.servicelogic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.net.layers.impl.InputData;
import com.rakovpublic.jneuropallium.worker.net.signals.InitInputWrapper;
import com.rakovpublic.jneuropallium.worker.net.signals.InputInitStrategyWrapper;
import com.rakovpublic.jneuropallium.worker.net.signals.OneToAllFirstLayerInputStrategy;
import com.rakovpublic.jneuropallium.worker.test.definitions.ioutils.TestInitInputDoubleSignal;
import com.rakovpublic.jneuropallium.worker.test.definitions.ioutils.TestInitInputIntSignal;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InputDataGenerator {
    public static void main(String [] args){
        List<InputData> inputData = new ArrayList<>();
        inputData.add(new InputData(new InitInputWrapper(new TestInitInputDoubleSignal()),false, new InputInitStrategyWrapper(new OneToAllFirstLayerInputStrategy()),1));
        inputData.add(new InputData(new InitInputWrapper(new TestInitInputIntSignal()),false, new InputInitStrategyWrapper(new OneToAllFirstLayerInputStrategy()),1));
        ObjectMapper mapper = new ObjectMapper();
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter("F:\\git\\inputdata"));
            writer.write(mapper.writeValueAsString(inputData));
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
