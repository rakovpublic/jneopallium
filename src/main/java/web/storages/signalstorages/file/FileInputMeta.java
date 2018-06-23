package web.storages.signalstorages.file;

import exceptions.ClassFromJSONIsNotExistsException;
import exceptions.SerializerForClassIsNotRegisteredException;
import synchronizer.utils.JSONHelper;
import synchronizer.utils.JSONHelperResult;
import web.signals.ISignal;
import web.storages.IInputMeta;
import web.storages.ISerializer;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FileInputMeta implements IInputMeta<String> {
    private File file;
    private HashMap<Class<? extends ISignal>,ISerializer<? extends ISignal,String>> map;

    public FileInputMeta(File file, HashMap<Class<? extends ISignal>, ISerializer<? extends ISignal, String>> map) {
        this.file = file;
        this.map = map;
    }

    @Override
    public <S extends ISignal> void registerSerializer(ISerializer<S, String> serializer, Class<S> clazz) {
        map.put(clazz,serializer);
    }

    @Override
    public HashMap<Long, List<ISignal>> readInputs(int layerId) {

        File ff= new File(file.getAbsolutePath()+File.pathSeparator+layerId);
        if(!ff.exists()){

        }
        //TODO:add input lock

        HashMap<Long,  List<ISignal>> result = new HashMap<>();
        StringBuilder sb=new StringBuilder();
        BufferedReader br=null;
        FileReader fr=null;
        try {
            fr=new FileReader(ff);
            br=new BufferedReader(fr);
            String content=null;
            while ((content=br.readLine())!=null) {
                sb.append(content);
            }
        }catch (IOException  ex){

        }finally {
            if(br!=null){
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(fr!=null){
                try {
                    fr.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        String json= sb.toString();
        int startIndex=json.indexOf('[');
        JSONHelperResult res=JSONHelper.getNextJSONObject(json,startIndex);
        while (res!=null){
            String className= res.getClassName();
            String jsonObject=res.getObject();
            Long neuronID=Long.parseLong(JSONHelper.extractJsonField(jsonObject,"neuronId"));
            startIndex=res.getIndex();
            try {
                Class cl=Class.forName(className);
                if(map.containsKey(cl)){
                ISerializer ser=map.get(cl);
                if(result.containsKey(neuronID)){
                    result.get(neuronID).add((ISignal) ser.deserialize(json));
                }else {
                    List<ISignal> l= new ArrayList<>();
                    l.add((ISignal) ser.deserialize(json));
                    result.put(neuronID,l);
                }
                    res=JSONHelper.getNextJSONObject(json,startIndex);
                }else {
                    throw new SerializerForClassIsNotRegisteredException("Serializer for class"+cl+"is not registered");
                }

            } catch (ClassNotFoundException e) {
              throw new ClassFromJSONIsNotExistsException("Class "+className+" from this json "+ jsonObject+" is not exists");
            }
        }
        ff.delete();
        return result;
    }

    @Override
    public void saveResults( HashMap<Long, List<ISignal>> signals,int layerId) {
        File dir= new File(file.getAbsolutePath()+File.pathSeparator+layerId);
        if(dir.exists()){
            mergeResults(signals,layerId);
        }else{
        StringBuilder  resultJson= new StringBuilder();
        resultJson.append("{\"inputs\":[");
        for(Long nrId:signals.keySet()){
            StringBuilder signal=new StringBuilder();
            signal.append("{\"neuronId\":\"");
            signal.append(nrId);
            signal.append("\",\"signal\":");
            for (ISignal s:signals.get(nrId)){
                ISerializer serializer=map.get(s.getCurrentClass());
                StringBuilder resSignal= new StringBuilder(signal);
                resSignal.append(serializer.serialize(resSignal.toString()));
                resSignal.append("},");
                resultJson.append(resSignal.toString());
            }

        }
        resultJson.deleteCharAt(resultJson.length()-1);
        resultJson.append("]}");
        try {
            dir.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedWriter bw = null;
        FileWriter fw = null;

        try {



            fw = new FileWriter(dir);
            bw = new BufferedWriter(fw);
            bw.write(resultJson.toString());

        } catch (IOException e) {

            e.printStackTrace();

        } finally {

            try {

                if (bw != null)
                    bw.close();

                if (fw != null)
                    fw.close();

            } catch (IOException ex) {

                ex.printStackTrace();

            }

        }
        }

    }

    @Override
    public void mergeResults(HashMap<Long, List<ISignal>> signals, int layerId) {
        File dir= new File(file.getAbsolutePath()+File.pathSeparator+layerId);
        if(!dir.exists()){
            saveResults(signals,layerId);
        }else {
            //TODO: add merging logic
        }
    }
}
