package web.storages.file;

import exceptions.IncorrectFilePathForStorage;
import web.signals.ISignal;
import web.storages.IInputMeta;
import web.storages.ILayerMeta;
import web.storages.ISerializer;
import web.storages.IStructMeta;

import java.io.File;
import java.util.HashMap;
import java.util.List;

public class FileStructMeta implements IStructMeta {
    private String path;
    private File file;
    private  IInputMeta inputMeta;
    private IInputMeta hiddenInputMeta;
    public FileStructMeta(String path  ) {
        this.path = path;
        this.file=new File(path);
        if(!isValidPath(file)){
            throw new IncorrectFilePathForStorage();
        }
    }
    private static Boolean isValidPath(File f){

        return f.exists()&&f.isDirectory();
    }
    @Override
    public List<ILayerMeta> getLayers() {
        for(File f:file.listFiles()){

        }
        return null;
    }

    @Override
    public List<IInputMeta> getInputs(String layerId) {
        return null;
    }

    @Override
    public void saveResults(String layerId, List<IInputMeta> meta) {

    }
}
