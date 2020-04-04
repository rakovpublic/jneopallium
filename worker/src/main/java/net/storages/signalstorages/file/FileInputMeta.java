package net.storages.signalstorages.file;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import exceptions.ClassFromJSONIsNotExistsException;
import exceptions.InputNotExistsException;
import exceptions.SerializerForClassIsNotRegisteredException;
import net.neuron.INeuron;
import net.signals.IResultSignal;
import net.signals.ISignal;
import net.storages.IInputMeta;
import net.storages.ISerializer;
import net.storages.filesystem.IFileSystem;
import net.storages.filesystem.IFileSystemItem;
import synchronizer.utils.DeserializationHelperResult;
import synchronizer.utils.JSONHelper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class FileInputMeta<S extends IFileSystemItem> implements IInputMeta<String> {

    //TODO: add step id to input path
    private S file;
    private IFileSystem<S> fileSystem;
    private Long stepID;

    public FileInputMeta(S file, IFileSystem<S> fs) {
        this.file = file;
        this.fileSystem = fs;
        this.stepID=0l;
    }


//TODO: test cleaning
    @Override
    public HashMap<Long, List<ISignal>> readInputs(int layerId) {

        S ff = fileSystem.getItem(file.getPath() + fileSystem.getFolderSeparator() + stepID+fileSystem.getFolderSeparator()+layerId);
        //new File(file.getAbsolutePath() + File.pathSeparator + layerId);
        if (!ff.exists()) {
            throw new InputNotExistsException("File " + ff.getPath() + " with input data for layer id " + layerId + " is not exists");
        }
        HashMap<Long, List<ISignal>> result = new HashMap<>();
        if (ff.isDirectory()) {
            for (IFileSystemItem fsiNeuron : fileSystem.listFiles(ff)) {
                JSONHelper jsonHelper = new JSONHelper();
                String json = fileSystem.read((S) fsiNeuron);
                JsonElement jelement = new JsonParser().parse(json);
                JsonObject jobject = jelement.getAsJsonObject();
                JsonArray jarray = jobject.getAsJsonArray("inputs");
                ObjectMapper mapper= new ObjectMapper();
                for(JsonElement jel:jarray){
                    Long neuronID= Long.parseLong(jel.getAsJsonObject().get("neuronId").getAsString());
                    JsonArray jsig = jel.getAsJsonObject().getAsJsonArray("signal");
                    for(JsonElement jek:jsig){
                    String cl=jek.getAsJsonObject().get("currentSignalClass").getAsString();
                    try {
                        ISignal signal= (ISignal) mapper.readValue(jek.getAsJsonObject().toString(),Class.forName(cl));
                        if (result.containsKey(neuronID)) {
                            result.get(neuronID).add(signal);
                        } else {
                            List<ISignal> l = new ArrayList<>();
                            l.add(signal);
                            result.put(neuronID, l);
                        }
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                        //TODO:Add logger
                    }
                    }
                }
            }
        }
        //TODO:add input lock
        return result;
    }

    @Override
    public List<ISignal> readInputsForNeuron(int layerId, Long neuronId) {
        return null;
    }

    @Override
    public void cleanInputs() {
        S ff = fileSystem.getItem(file.getPath() + fileSystem.getFolderSeparator());
        //new File(file.getAbsolutePath() + File.pathSeparator + layerId);
        if (!ff.exists()) {
            throw new InputNotExistsException("File " + ff.getPath() + " with input data for layer id  is not exists");
        }

        if (ff.isDirectory()) {
            for (IFileSystemItem fsiNeuron : fileSystem.listFiles(ff)) {
                if (!fsiNeuron.getPath().equals(fileSystem.getItem(file.getPath() + fileSystem.getFolderSeparator() + 0))) {
                    fileSystem.deleteFilesFromDirectory(ff);
                }

            }

        }

    }

    @Override
    public void saveResults(HashMap<Long, List<ISignal>> signals, int layerId) {
        //File dir = new File(file.getAbsolutePath() + File.pathSeparator + layerId);
        saveResultToStep( signals, layerId, stepID);


    }

    private void saveResultToStep(HashMap<Long, List<ISignal>> signals, int layerId, Long stepID){
        S dir = fileSystem.getItem(file.getPath() + fileSystem.getFolderSeparator() + stepID+ fileSystem.getFolderSeparator()+layerId+ fileSystem.getFolderSeparator() +  1);
        S layerDir=fileSystem.getItem(file.getPath() + fileSystem.getFolderSeparator() + stepID+ fileSystem.getFolderSeparator()+layerId);
        if(!layerDir.exists()){
            fileSystem.createFolder(layerDir);
        }
        if (dir.exists()) {
            mergeResults(signals, dir.getPath());
        } else {
            save(signals, dir);
        }

    }

    @Override
    public void mergeResults(HashMap<Long, List<ISignal>> signals, String path) {
        S dir = fileSystem.getItem(path);
        if (!dir.exists()) {
            save(signals, dir);
        } else {

            mergeResults(signals, path + 1);
        }
    }

    @Override
    public IResultSignal getDesiredResult() {
        IResultSignal obj = null;
        S ff = fileSystem.getItem(file.getPath() + fileSystem.getFolderSeparator() + "result");
        if (ff.exists()) {
            String str = fileSystem.read(ff);
            if (str == null) {
                return null;
            }
            ObjectMapper mapper= new ObjectMapper();
            try {
                JsonElement jelement = new JsonParser().parse(str);
                JsonObject jobject = jelement.getAsJsonObject();
                String cl =jobject.getAsJsonPrimitive("currentSignalClass").getAsString();
                obj = (IResultSignal) mapper.readValue(jelement.getAsString(),Class.forName(cl));;
            } catch (Exception ex) {
                ex.printStackTrace();
                //TODO:Add logger
            }
        }
        return obj;
    }

    @Override
    public void copySignalToNextStep(int layerId, Long neuronId, ISignal signal) {
        HashMap<Long, List<ISignal>> struct= new HashMap<>();
        List<ISignal> signals= new LinkedList<>();
        signals.add(signal);
        struct.put(neuronId,signals);
        saveResultToStep(struct,layerId,stepID+1);
    }

    @Override
    public void nextStep() {

        stepID+=1;
        S ff = fileSystem.getItem(file.getPath() + fileSystem.getFolderSeparator() + stepID);
        fileSystem.createFolder(ff);
    }

    @Override
    public void copyInputsToNextStep() {
        fileSystem.copy(fileSystem.getItem(file.getPath() + fileSystem.getFolderSeparator() + stepID+fileSystem.getFolderSeparator()+0),
                fileSystem.getItem(file.getPath() + fileSystem.getFolderSeparator() +stepID+1+ fileSystem.getFolderSeparator()+0));
    }

    private void save(HashMap<Long, List<ISignal>> signals, S path) {
        StringBuilder resultJson = new StringBuilder();
        ObjectMapper mapper= new ObjectMapper();
        resultJson.append("{\"inputs\":[");
        for (Long nrId : signals.keySet()) {
            StringBuilder signal = new StringBuilder();
            signal.append("{\"neuronId\":\"");
            signal.append(nrId);
            signal.append("\",\"signal\":[");
            for (ISignal s : signals.get(nrId)) {
                String serializedSignal= null;
                try {
                    serializedSignal = mapper.writeValueAsString(s);
                    if(serializedSignal!=null){
                        signal.append(serializedSignal);
                        signal.append(",");
                    }
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                    //TODO: add logging
                }

            }
            if(signals.get(nrId).size()>0){
                signal.deleteCharAt(signal.length() - 1);
            }
            signal.append("]},");
            resultJson.append(signal.toString());
        }
        resultJson.deleteCharAt(resultJson.length() - 1);
        resultJson.append("]}");
        fileSystem.writeCreate(resultJson.toString(), path);
    }
}
