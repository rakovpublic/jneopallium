package net.storages.file;

import net.neuron.INeuron;
import net.storages.ILayerMeta;
import net.storages.INeuronMeta;
import net.storages.filesystem.IFileSystem;
import net.storages.filesystem.IFileSystemItem;
import synchronizer.utils.JSONHelper;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FileLayerMeta<S extends IFileSystemItem> implements ILayerMeta {
    protected S file;
    protected IFileSystem<S> fileSystem;
    FileLayerMeta(S file, IFileSystem<S> fs) {
        this.file = file;
        this.fileSystem=fs;
    }

    @Override
    public int getID() {
        String layer= fileSystem.read(file);
        JSONHelper helper= new JSONHelper();
        return Integer.parseInt(helper.extractField(layer,"layerID"));
    }

    @Override
    public List<? extends INeuron> getNeurons() {
        String layer= fileSystem.read(file);
        JSONHelper helper= new JSONHelper();
        List<INeuron> result= new ArrayList<>();
        for(INeuron ner:getNeurons(helper.extractField(layer,"neurons"))){
            result.add(ner);
        }
        return result;
    }

    @Override
    public void saveNeurons(Collection<? extends INeuron> neuronMetas) {
        StringBuilder sb= new StringBuilder();
        sb.append("{\"layerID\":\"");
        sb.append(getID()+"\",");
        sb.append("\"layerSize\":\"");
        sb.append(getSize()+"\",");
        sb.append("\"neurons\":\"");
        fileSystem.rewrite(sb.toString(),file);
        fileSystem.writeUpdateObjects(neuronMetas.toArray(),file);
        fileSystem.writeUpdateToEnd("\"}",file);

    }


    @Override
    public void dumpLayer() {

    }

    @Override
    public Long getSize() {
        String layer= fileSystem.read(file);
        JSONHelper helper= new JSONHelper();
        return Long.parseLong(helper.extractField(layer,"layerSize"));
    }

    private INeuron[] getNeurons(String str) {
        INeuron[] obj=null;
        try {
            byte b[] = str.getBytes();
            ByteArrayInputStream bi = new ByteArrayInputStream(b);
            ObjectInputStream si = new ObjectInputStream(bi);
            obj = (INeuron[]) si.readObject();
        }catch (Exception ex){
            //TODO:Add logger
        }
        return obj;

    }

}
