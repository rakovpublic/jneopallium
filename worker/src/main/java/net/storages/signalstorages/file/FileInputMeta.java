package net.storages.signalstorages.file;

import exceptions.ClassFromJSONIsNotExistsException;
import exceptions.InputNotExistsException;
import exceptions.SerializerForClassIsNotRegisteredException;
import synchronizer.utils.JSONHelper;
import synchronizer.utils.DeserializationHelperResult;
import net.signals.ISignal;
import net.storages.IInputMeta;
import net.storages.ISerializer;
import net.storages.filesystem.IFileSystem;
import net.storages.filesystem.IFileSystemItem;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FileInputMeta<S extends IFileSystemItem> implements IInputMeta<String> {

    //TODO: add step id to input path
    private S file;
    private HashMap<Class<? extends ISignal>, ISerializer<? extends ISignal, String>> map;
    private IFileSystem<S> fileSystem;

    public FileInputMeta(S file, HashMap<Class<? extends ISignal>, ISerializer<? extends ISignal, String>> map,IFileSystem<S> fs) {
        this.file = file;
        this.map = map;
        this.fileSystem=fs;
    }

    @Override
    public <S extends ISignal> void registerSerializer(ISerializer<S, String> serializer, Class<S> clazz) {
        map.put(clazz, serializer);
    }

    @Override
    public HashMap<Long, List<ISignal>> readInputs(int layerId) {

        S ff = fileSystem.getItem(file.getPath()+fileSystem.getFolderSeparator()+layerId);
        //new File(file.getAbsolutePath() + File.pathSeparator + layerId);
        if (!ff.exists()) {
            throw new InputNotExistsException("File " +ff.getPath()+" with input data for layer id "+layerId+" is not exists");
        }
        HashMap<Long, List<ISignal>> result = new HashMap<>();
        if(ff.isDirectory()){
            for(IFileSystemItem fsiNeuron:fileSystem.listFiles(ff)){
                JSONHelper jsonHelper= new JSONHelper();
                String json =fileSystem.read((S) fsiNeuron);
                int startIndex = json.indexOf('[');
                DeserializationHelperResult res = jsonHelper.getNextObject(json, startIndex);
                while (res != null) {
                    String className = res.getClassName();
                    String jsonObject = res.getObject();
                    Long neuronID = Long.parseLong(jsonHelper.extractField(jsonObject, "neuronId"));
                    startIndex = res.getIndex();
                    try {
                        Class cl = Class.forName(className);
                        if (map.containsKey(cl)) {
                            ISerializer ser = map.get(cl);
                            if (result.containsKey(neuronID)) {
                                result.get(neuronID).add((ISignal) ser.deserialize(json));
                            } else {
                                List<ISignal> l = new ArrayList<>();
                                l.add((ISignal) ser.deserialize(json));
                                result.put(neuronID, l);
                            }
                            res = jsonHelper.getNextObject(json, startIndex);
                        } else {
                            throw new SerializerForClassIsNotRegisteredException("Serializer for class" + cl + "is not registered");
                        }

                    } catch (ClassNotFoundException e) {
                        throw new ClassFromJSONIsNotExistsException("Class " + className + " from this json " + jsonObject + " is not exists");
                    }
                }


            }
        }
        //TODO:add input lock
        return result;
    }

    @Override
    public void cleanInputs() {
        S ff = fileSystem.getItem(file.getPath()+fileSystem.getFolderSeparator());
        //new File(file.getAbsolutePath() + File.pathSeparator + layerId);
        if (!ff.exists()) {
            throw new InputNotExistsException("File " +ff.getPath()+" with input data for layer id  is not exists");
        }

        if(ff.isDirectory()){
            for(IFileSystemItem fsiNeuron:fileSystem.listFiles(ff)){
                if(!fsiNeuron.getPath().equals(fileSystem.getItem(file.getPath()+fileSystem.getFolderSeparator()+0))){
                    fileSystem.deleteFilesFromDirectory(ff);
                }

            }

            }

    }

    @Override
    public void saveResults(HashMap<Long, List<ISignal>> signals, int layerId) {
        //File dir = new File(file.getAbsolutePath() + File.pathSeparator + layerId);
        S dir = fileSystem.getItem(file.getPath()+fileSystem.getFolderSeparator()+layerId+fileSystem.getFolderSeparator()+1);
        if (dir.exists()) {
            mergeResults(signals, dir.getPath());
        } else {
            save(signals,dir);
        }

    }

    @Override
    public void mergeResults(HashMap<Long, List<ISignal>> signals, String path) {
        S dir = fileSystem.getItem(path);
        if (!dir.exists()) {
            save(signals, dir);
        } else {

            mergeResults(signals,path+1);
        }
    }

    @Override
    public Object getDesiredResult()  {
        Object obj=null;
        S ff = fileSystem.getItem(file.getPath()+fileSystem.getFolderSeparator()+"result");
        if(ff.exists()){
            String str=fileSystem.read(ff);
            if(str==null){
                return null;
            }

            try {
                byte b[] = str.getBytes();
                ByteArrayInputStream bi = new ByteArrayInputStream(b);
                ObjectInputStream si = new ObjectInputStream(bi);
                obj =  si.readObject();
            }catch (Exception ex){
                ex.printStackTrace();
                //TODO:Add logger
            }
        }
        return obj;
    }

    @Override
    public void copySignalToNextStep(int layerId, Long neuronId, ISignal signal) {
        //TODO: implement
    }

    private void save(HashMap<Long, List<ISignal>> signals,S path){
        StringBuilder resultJson = new StringBuilder();
        resultJson.append("{\"inputs\":[");
        for (Long nrId : signals.keySet()) {
            StringBuilder signal = new StringBuilder();
            signal.append("{\"neuronId\":\"");
            signal.append(nrId);
            signal.append("\",\"signal\":");
            for (ISignal s : signals.get(nrId)) {
                ISerializer serializer = map.get(s.getCurrentClass());
                StringBuilder resSignal = new StringBuilder(signal);
                resSignal.append(serializer.serialize(resSignal.toString()));
                resSignal.append("},");
                resultJson.append(resSignal.toString());
            }

        }
        resultJson.deleteCharAt(resultJson.length() - 1);
        resultJson.append("]}");
        fileSystem.writeCreate(resultJson.toString(),path);
    }
}
