/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions.ioutils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;
import com.rakovpublic.jneuropallium.worker.test.definitions.functionallogic.InputIntSignal;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class TestInitInputIntSignal implements IInitInput {
    public String clazz = "com.rakovpublic.jneuropallium.worker.test.definitions.ioutils.TestInitInputIntSignal";
    public String path="F:\\git\\intInput";
    public ProcessingFrequency defaultProcessingFrequency;
    public HashMap<String, List<IResultSignal>> desiredResults;
    public String name;

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public TestInitInputIntSignal() {
        String description = "test signal";
        defaultProcessingFrequency =new ProcessingFrequency(1l,1);
        desiredResults = new HashMap<>();
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public List<IInputSignal> readSignals() {
        File file = new File("F:\\git\\intInput");
        StringBuilder builder = new StringBuilder();
        // Declaring a string variable
        String st;

        BufferedReader br
                = null;
        try {
            br = new BufferedReader(new FileReader(file));
            while ((st = br.readLine()) != null){
                builder.append(st);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }



        ObjectMapper mapper = new ObjectMapper();
        List<IInputSignal> result = null;
        try {
            result = mapper.readValue(builder.toString(), SignalArray.class).getSignals().stream().map(a->a.getSignal()).collect(Collectors.toList());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return result;
    }


    @Override
    public String getName() {
        return name;
    }

    @Override
    public HashMap<String, List<IResultSignal>> getDesiredResults() {
        return new HashMap<>();
    }

    @Override
    public ProcessingFrequency getDefaultProcessingFrequency() {
        return new ProcessingFrequency(1l,1);
    }

    public void setDefaultProcessingFrequency(ProcessingFrequency defaultProcessingFrequency) {
        this.defaultProcessingFrequency = defaultProcessingFrequency;
    }

    public void setDesiredResults(HashMap<String, List<IResultSignal>> desiredResults) {
        this.desiredResults = desiredResults;
    }
}
