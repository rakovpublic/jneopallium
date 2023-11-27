/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalMerger;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NeuronParser {
    private static final Logger logger = LogManager.getLogger(NeuronParser.class);

    public static List<INeuron> parseNeurons(String json) {
        List<INeuron> result = new ArrayList<>();
        JsonElement jelement = new JsonParser().parse(json);
        JsonObject jobject = jelement.getAsJsonObject();
        JsonArray jarray = jobject.getAsJsonArray("neurons");
        ObjectMapper mapper = new ObjectMapper();
        for (JsonElement jel : jarray) {
            String cl = jel.getAsJsonObject().getAsJsonPrimitive("currentNeuronClass").getAsString();
            try {
                INeuron neuron = (INeuron) mapper.readValue(jel.getAsJsonObject().toString(), Class.forName(cl));
                HashMap<Class<?>, ISignalProcessor> p = new HashMap<>();
                for (Map.Entry<String, JsonElement> e : jel.getAsJsonObject().getAsJsonObject("processorMap").entrySet()) {
                    String cc = e.getValue().getAsJsonObject().getAsJsonPrimitive("signalProcessorClass").getAsString();
                    neuron.addSignalProcessor((Class<? extends ISignal>) Class.forName(e.getKey()), (ISignalProcessor) mapper.readValue(e.getValue().getAsJsonObject().toString(), Class.forName(cc)));
                }
                for (Map.Entry<String, JsonElement> e : jel.getAsJsonObject().getAsJsonObject("mergerMap").entrySet()) {
                    String cc = e.getValue().getAsJsonObject().getAsJsonPrimitive("signalMergerClass").getAsString();
                    neuron.addSignalMerger((Class<? extends ISignal>) Class.forName(e.getKey()), (ISignalMerger) mapper.readValue(e.getValue().getAsJsonObject().toString(), Class.forName(cc)));
                }
                result.add(neuron);
            } catch (IOException | ClassNotFoundException e) {
                logger.error("cannot parse neuron from json " + jel.getAsJsonObject().toString(), e);
            }
        }
        return result;
    }
}
