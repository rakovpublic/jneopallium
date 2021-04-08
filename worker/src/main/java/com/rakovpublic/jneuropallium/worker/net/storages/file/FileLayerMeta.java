package com.rakovpublic.jneuropallium.worker.net.storages.file;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rakovpublic.jneuropallium.worker.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.neuron.ISignalMerger;
import com.rakovpublic.jneuropallium.worker.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.net.storages.filesystem.IFileSystem;
import com.rakovpublic.jneuropallium.worker.net.storages.filesystem.IFileSystemItem;
import com.rakovpublic.jneuropallium.worker.synchronizer.utils.JSONHelper;

import java.io.*;
import java.util.*;

public class FileLayerMeta<S extends IFileSystemItem> implements ILayerMeta {
    protected S file;
    protected IFileSystem<S> fileSystem;
    protected List<? extends INeuron> neurons;

    FileLayerMeta(S file, IFileSystem<S> fs) {
        this.file = file;
        this.fileSystem = fs;
    }

    @Override
    public int getID() {
        String layer = fileSystem.read(file);
        JSONHelper helper = new JSONHelper();
        return Integer.parseInt(helper.extractField(layer, "layerId"));
    }

    @Override
    public List<? extends INeuron> getNeurons() {
        String layer = fileSystem.read(file);
        List<INeuron> result = new ArrayList<>();
        JsonElement jelement = new JsonParser().parse(layer);
        JsonObject jobject = jelement.getAsJsonObject();
        JsonArray jarray = jobject.getAsJsonArray("neurons");
        ObjectMapper mapper= new ObjectMapper();
        for(JsonElement jel:jarray){
            String cl=jel.getAsJsonObject().getAsJsonPrimitive("currentNeuronClass").getAsString();
            try {
                INeuron neuron= (INeuron) mapper.readValue(jel.getAsJsonObject().toString(),Class.forName(cl));
                HashMap<Class<?>, ISignalProcessor> p= new HashMap<>();
                for(Map.Entry<String,JsonElement> e: jel.getAsJsonObject().getAsJsonObject("processorMap").entrySet()){
                    String cc= e.getValue().getAsJsonObject().getAsJsonPrimitive("signalProcessorClass").getAsString();
                    neuron.addSignalProcessor((Class<? extends ISignal>) Class.forName(e.getKey()),(ISignalProcessor) mapper.readValue(e.getValue().getAsJsonObject().toString(),Class.forName(cc)));
                }
                for(Map.Entry<String,JsonElement> e: jel.getAsJsonObject().getAsJsonObject("mergerMap").entrySet()){
                    String cc= e.getValue().getAsJsonObject().getAsJsonPrimitive("signalMergerClass").getAsString();
                    neuron.addSignalMerger((Class<? extends ISignal>) Class.forName(e.getKey()),(ISignalMerger) mapper.readValue(e.getValue().getAsJsonObject().toString(),Class.forName(cc)));
                }
                result.add(neuron);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                //TODO:Add logger
            }
        }
        return result;
    }

    @Override
    public INeuron getNeuronByID(Long id) {
        for (INeuron ner : getNeurons()) {
            if (ner.getId() == id) {
                return ner;
            }
        }
        return null;
    }

    @Override
    public void saveNeurons(Collection<? extends INeuron> neuronMetas) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"layerID\":\"");
        sb.append(getID() + "\",");
        sb.append("\"layerSize\":\"");
        sb.append(getSize() + "\",");
        sb.append("\"neurons\":");
        ObjectMapper mapper= new ObjectMapper();
        String serializedObject = null;
        try {
            serializedObject = mapper.writeValueAsString(neuronMetas);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            //TODO:Add logger
        }
        sb.append(serializedObject);
        sb.append("}");
    }


    @Override
    public void dumpLayer() {
        if (neurons == null || neurons.size() == 0) {
            getNeurons();
        }
        saveNeurons(neurons);
    }

    @Override
    public Long getSize() {
        String layer = fileSystem.read(file);
        JSONHelper helper = new JSONHelper();
        return Long.parseLong(helper.extractField(layer, "layerSize"));
    }
}
