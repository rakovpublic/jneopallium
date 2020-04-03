package net.storages.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.neuron.INeuron;
import net.neuron.IResultNeuron;
import net.storages.IResultLayerMeta;
import net.storages.filesystem.IFileSystem;
import net.storages.filesystem.IFileSystemItem;
import synchronizer.utils.JSONHelper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

public class FileResultLayerMeta extends FileLayerMeta implements IResultLayerMeta {
    FileResultLayerMeta(IFileSystemItem file, IFileSystem fs) {
        super(file, fs);
    }

    @Override
    public List<? extends IResultNeuron> getNeurons() {
        String layer = fileSystem.read(file);
        List<IResultNeuron> result = new ArrayList<>();
        JsonElement jelement = new JsonParser().parse(layer);
        JsonObject jobject = jelement.getAsJsonObject();
        JsonArray jarray = jobject.getAsJsonArray("neurons");
        ObjectMapper mapper= new ObjectMapper();
        for(JsonElement jel:jarray){
            String cl=jel.getAsJsonObject().get("currentNeuronClass").getAsString();
            try {
                IResultNeuron neuron= (IResultNeuron) mapper.readValue(jel.getAsString(),Class.forName(cl));
                result.add(neuron);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                //TODO:Add logger
            }
        }
        return result;
    }
}
