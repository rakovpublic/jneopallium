package com.rakovpublic.jneuropallium.worker.net.layers.impl.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rakovpublic.jneuropallium.worker.net.layers.IResultLayerMeta;
import com.rakovpublic.jneuropallium.worker.net.neuron.IActivationFunction;
import com.rakovpublic.jneuropallium.worker.net.neuron.IResultNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalMerger;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.filesystem.IStorage;
import com.rakovpublic.jneuropallium.worker.net.storages.filesystem.IStorageItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileResultLayerMeta extends FileLayerMeta implements IResultLayerMeta {
    private static final Logger logger = LogManager.getLogger(FileResultLayerMeta.class);

    FileResultLayerMeta(IStorageItem file, IStorage fs) {
        super(file, fs);
    }

    @Override
    public List<? extends IResultNeuron> getNeurons() {
        String layer = fileSystem.read(file);
        List<IResultNeuron> result = new ArrayList<>();
        JsonElement jelement = new JsonParser().parse(layer);
        JsonObject jobject = jelement.getAsJsonObject();
        JsonArray jarray = jobject.getAsJsonArray("neurons");
        ObjectMapper mapper = new ObjectMapper();
        for (JsonElement jel : jarray) {
            String cl = jel.getAsJsonObject().getAsJsonPrimitive("currentNeuronClass").getAsString();
            try {
                IResultNeuron neuron = (IResultNeuron) mapper.readValue(jel.getAsJsonObject().toString(), Class.forName(cl));
                HashMap<Class<?>, ISignalProcessor> p = new HashMap<>();
                for (Map.Entry<String, JsonElement> e : jel.getAsJsonObject().getAsJsonObject("activationFunctions").entrySet()) {
                    String cc = e.getValue().getAsJsonObject().getAsJsonPrimitive("activationFunctionClass").getAsString();
                    neuron.addActivationFunction((Class<? extends ISignal>) Class.forName(e.getKey()), (IActivationFunction) mapper.readValue(e.getValue().getAsJsonObject().toString(), Class.forName(cc)));
                }
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
                logger.error("cannot parse neuron from json", e);
            }
        }
        return result;
    }
}
