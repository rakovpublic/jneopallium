package com.rakovpublic.jneuropallium.worker.net.layers.impl.file;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.net.layers.impl.LayerMetaParam;
import com.rakovpublic.jneuropallium.worker.net.layers.impl.LayerMove;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.storages.filesystem.IStorage;
import com.rakovpublic.jneuropallium.worker.net.storages.filesystem.IStorageItem;
import com.rakovpublic.jneuropallium.worker.util.NeuronParser;
import com.rakovpublic.jneuropallium.worker.util.json.JSONHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class FileLayerMeta<S extends IStorageItem> implements ILayerMeta {
    private static final Logger logger = LogManager.getLogger(FileLayerMeta.class);
    protected S file;
    protected IStorage<S> fileSystem;
    protected List<INeuron> neurons;
    protected HashMap<String, LayerMetaParam> layerMetaParams;
    protected  List<LayerMove> layerMoves;

    FileLayerMeta(S file, IStorage<S> fs) {
        this.file = file;
        this.fileSystem = fs;
        layerMoves=  new LinkedList<>();
    }

    @Override
    public HashMap<String, LayerMetaParam> getLayerMetaParams() {
        if (layerMetaParams == null) {
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
                layerMetaParams.put(paramName, layerMetaParam);
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
        HashMap<Long, HashMap<Integer,List<Long>>> moves = layerMove.getMovingMap();
        for(Long targetNeuronId:moves.keySet()){
            INeuron neuron = getNeuronByID(targetNeuronId);
            neuron.getAxon().moveConnection(layerMove,neuron.getLayer().getId(),targetNeuronId);
        }
    }

    @Override
    public List<INeuron> getNeurons() {
        String layer = fileSystem.read(file);
        return NeuronParser.parseNeurons(layer);
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
    public void removeNeuron(Long neuron) {
        for (INeuron iNeuron : neurons){
            if (neuron.equals(iNeuron.getId())){
                neurons.remove(iNeuron);
                break;
            }
        }
    }

    @Override
    public void addNeuron(INeuron neuron) {
        neurons.add(neuron);
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
            neurons = getNeurons();
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
