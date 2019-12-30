package net.storages.file;

import net.neuron.INeuron;
import net.neuron.IResultNeuron;
import net.storages.ILayerMeta;
import net.storages.IResultLayerMeta;
import net.storages.filesystem.IFileSystem;
import net.storages.filesystem.IFileSystemItem;
import synchronizer.utils.JSONHelper;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

public class FileResultLayerMeta extends FileLayerMeta implements IResultLayerMeta {
    FileResultLayerMeta(IFileSystemItem file, IFileSystem fs) {
        super(file, fs);
    }
    @Override
    public List<? extends IResultNeuron> getNeurons() {
        String layer= fileSystem.read(file);
        JSONHelper helper= new JSONHelper();
        List<IResultNeuron> result= new ArrayList<>();
        for(IResultNeuron ner:getNeurons(helper.extractField(layer,"neurons"))){
            result.add(ner);
        }
        return result;
    }
    private IResultNeuron[] getNeurons(String str) {
        IResultNeuron[] obj=null;
        try {
            byte b[] = str.getBytes();
            ByteArrayInputStream bi = new ByteArrayInputStream(b);
            ObjectInputStream si = new ObjectInputStream(bi);
            obj = (IResultNeuron[]) si.readObject();
        }catch (Exception ex){
            //TODO:Add logger
        }
        return obj;

    }
}
