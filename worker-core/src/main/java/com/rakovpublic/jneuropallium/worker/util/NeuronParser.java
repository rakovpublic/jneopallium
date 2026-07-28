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
import com.rakovpublic.jneuropallium.worker.net.neuron.IResultNeuron;
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
    /**
     * Neurons expose derived, read-only properties - and a stored neuron is written by whichever
     * node processed it last, so its document carries whatever that node's serializer emitted.
     * Reading has to tolerate properties that have no setter, otherwise a neuron cannot survive
     * a round trip through a shared store.
     */
    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = new ObjectMapper()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static List<INeuron> parseNeurons(String json) {
        List<INeuron> result = new ArrayList<>();
        JsonElement jelement = JsonParser.parseString(json);
        JsonObject jobject = jelement.getAsJsonObject();
        JsonArray jarray = jobject.getAsJsonArray("neurons");
        ObjectMapper mapper = MAPPER;
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

    public static List<IResultNeuron> parseResultNeurons(String json) {
        List<IResultNeuron> result = new ArrayList<>();
        JsonElement jelement = JsonParser.parseString(json);
        JsonObject jobject = jelement.getAsJsonObject();
        JsonArray jarray = jobject.getAsJsonArray("neurons");
        ObjectMapper mapper = MAPPER;
        for (JsonElement jel : jarray) {
            String cl = jel.getAsJsonObject().getAsJsonPrimitive("currentNeuronClass").getAsString();
            try {
                IResultNeuron neuron = (IResultNeuron) mapper.readValue(jel.getAsJsonObject().toString(), Class.forName(cl));
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

    public static INeuron parseNeuron(String json) {
        List<INeuron> result = new ArrayList<>();
        JsonElement jelement = JsonParser.parseString(json);
        JsonObject jobject = jelement.getAsJsonObject();
        ObjectMapper mapper = MAPPER;

        INeuron neuron = null;

        String cl = jelement.getAsJsonObject().getAsJsonPrimitive("currentNeuronClass").getAsString();
        try {
            neuron = (INeuron) mapper.readValue(jelement.getAsJsonObject().toString(), Class.forName(cl));
            HashMap<Class<?>, ISignalProcessor> p = new HashMap<>();
            for (Map.Entry<String, JsonElement> e : jelement.getAsJsonObject().getAsJsonObject("processorMap").entrySet()) {
                String cc = e.getValue().getAsJsonObject().getAsJsonPrimitive("signalProcessorClass").getAsString();
                neuron.addSignalProcessor((Class<? extends ISignal>) Class.forName(e.getKey()), (ISignalProcessor) mapper.readValue(e.getValue().getAsJsonObject().toString(), Class.forName(cc)));
            }
            for (Map.Entry<String, JsonElement> e : jelement.getAsJsonObject().getAsJsonObject("mergerMap").entrySet()) {
                String cc = e.getValue().getAsJsonObject().getAsJsonPrimitive("signalMergerClass").getAsString();
                neuron.addSignalMerger((Class<? extends ISignal>) Class.forName(e.getKey()), (ISignalMerger) mapper.readValue(e.getValue().getAsJsonObject().toString(), Class.forName(cc)));
            }
            result.add(neuron);
        } catch (IOException | ClassNotFoundException e) {
            logger.error("cannot parse neuron from json " + jelement.getAsJsonObject().toString(), e);
        }

        return neuron;
    }
}
