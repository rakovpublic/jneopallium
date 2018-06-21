package web.storages.file;

import exceptions.IncorrectFilePathForStorage;
import web.storages.IInputMeta;
import web.storages.ILayerMeta;
import web.storages.IStorageMeta;

import java.io.File;
import java.util.List;

public class FileStorageMeta implements IStorageMeta {
    private String path;
    private File file;

    public FileStorageMeta(String path) {
        this.path = path;
        this.file=new File(path);
        if(!isValidPath(file)){
            throw new IncorrectFilePathForStorage();
        }
    }
    private static Boolean isValidPath(File f){
        return f.isDirectory();
    }

    @Override
    public List<ILayerMeta> getLayers() {
        return null;
    }

    @Override
    public IInputMeta getInput() {
        return null;
    }
}
