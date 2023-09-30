package com.rakovpublic.jneuropallium.worker.net.storages.file;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rakovpublic.jneuropallium.worker.net.layers.LayerMetaParam;
import com.rakovpublic.jneuropallium.worker.net.layers.LayerMove;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.net.storages.filesystem.IStorage;
import com.rakovpublic.jneuropallium.worker.net.storages.filesystem.IStorageItem;
import com.rakovpublic.jneuropallium.worker.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.neuron.ISignalMerger;
import com.rakovpublic.jneuropallium.worker.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.synchronizer.utils.JSONHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;

public class FileLayerMeta<S extends IStorageItem> implements ILayerMeta {
    private static final Logger logger = LogManager.getLogger(FileLayerMeta.class);
    protected S file;
    protected IStorage<S> fileSystem;
    protected List<INeuron> neurons;
    protected HashMap<String, LayerMetaParam> layerMetaParams;

    FileLayerMeta(S file, IStorage<S> fs) {
        this.file = file;
        this.fileSystem = fs;
    }
    //TODO: add implementation
    @Override
    public HashMap<String, LayerMetaParam> getLayerMetaParams() {
        if(layerMetaParams==null){
            layerMetaParams = new HashMap<>();
            String layer = fileSystem.read(file);
            JsonElement jelement = new JsonParser().parse(layer);
            JsonObject jobject = jelement.getAsJsonObject();
            ObjectMapper mapper = new ObjectMapper();
            for (Map.Entry<String, JsonElement> e : jobject.getAsJsonObject().getAsJsonObject("metaParams").entrySet()) {
                String paramName = e.getKey();
                String cc = e.getValue().getAsJsonObject().getAsJsonPrimitive("paramClass").getAsString();
                LayerMetaParam layerMetaParam = null;
                try {
                    layerMetaParam = new LayerMetaParam(mapper.readValue(e.getValue().getAsJsonObject().getAsJsonObject("param").toString(), Class.forName(cc)));
                } catch (ClassNotFoundException | JsonProcessingException ex) {
                    logger.error("cannot parse meta params from json " + jobject.getAsJsonObject().toString(), ex);
                }
                layerMetaParams.put(paramName,layerMetaParam);
            }
        }
        return layerMetaParams;
    }

    @Override
    public void setLayerMetaParams(HashMap<String, LayerMetaParam> metaParams) {
        layerMetaParams = metaParams;
    }

    @Override
    public int getID() {
        String layer = fileSystem.read(file);
        JSONHelper helper = new JSONHelper();
        return Integer.parseInt(helper.extractField(layer, "layerId"));
    }

    @Override
    public void addLayerMove(LayerMove layerMove) {
        //TODO: add implementation
    }

    @Override
    public List<INeuron> getNeurons() {
        String layer = fileSystem.read(file);
        List<INeuron> result = new ArrayList<>();
        JsonElement jelement = new JsonParser().parse(layer);
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
    public void saveNeurons(List<INeuron> neuronMetas) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"layerID\":\"");
        sb.append(getID() + "\",");
        sb.append("\"layerSize\":\"");
        sb.append(getSize() + "\",");
        sb.append("\"neurons\":");
        ObjectMapper mapper = new ObjectMapper();
        String serializedObject = null;
        try {
            serializedObject = mapper.writeValueAsString(neuronMetas);
        } catch (JsonProcessingException e) {
            logger.error("cannot save  neurons to json ", e);
        }
        sb.append(serializedObject);
        sb.append(",\"metaParams\":");
        String serializedMetaParams = null;
        try {
            serializedMetaParams = mapper.writeValueAsString(layerMetaParams);
        } catch (JsonProcessingException e) {
            logger.error("cannot save  neurons to json ", e);
        }
        sb.append(serializedMetaParams);
        sb.append("}");
        fileSystem.rewrite(sb.toString(), file);
    }


    @Override
    public void dumpLayer() {
        if (neurons == null || neurons.size() == 0) {
            neurons= getNeurons();
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
