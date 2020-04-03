package net.storages.file;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.neuron.INeuron;
import net.neuron.IResultNeuron;
import net.neuron.impl.Neuron;
import net.storages.ILayerMeta;
import net.storages.filesystem.IFileSystem;
import net.storages.filesystem.IFileSystemItem;
import sample.SimpleNeuron;
import synchronizer.utils.JSONHelper;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

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
        return Integer.parseInt(helper.extractField(layer, "layerID"));
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
            String cl=jel.getAsJsonObject().get("currentNeuronClass").getAsString();
            try {
                INeuron neuron= (INeuron) mapper.readValue(jel.getAsString(),Class.forName(cl));
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
