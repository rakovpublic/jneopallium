package net.storages.file;

import exceptions.LayersFolderIsEmptyOrNotExistsException;
import net.storages.IResultLayerMeta;
import net.storages.filesystem.IFileSystem;
import net.storages.ILayerMeta;
import net.storages.ILayersMeta;
import net.storages.filesystem.IFileSystemItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FileLayersMeta<S extends IFileSystemItem> implements ILayersMeta {
    private S file;
    private IFileSystem fileSystem;


    public FileLayersMeta(S file, IFileSystem<S> fs) {
        this.fileSystem=fs;
        this.file = file;
    }

    @Override
    public List<ILayerMeta> getLayers() {
       // S layersDir = fileSystem.getItem(file+ fileSystem.getFolderSeparator()+"layers");
                //new File(file.getAbsolutePath() + File.pathSeparator + "layers");
        List<ILayerMeta> res = new ArrayList<>();
        List<S> temp = new ArrayList<>();
        if (!file.exists() || !file.isDirectory()) {
            throw new LayersFolderIsEmptyOrNotExistsException();
        }

        temp= fileSystem.listFiles(file);
        temp.removeIf(iFileSystemItem -> {
            if(!iFileSystemItem.isDirectory()){
                return true;
            }
            return false;
        });
        Collections.sort(temp, new Comparator<IFileSystemItem>() {
            @Override
            public int compare(IFileSystemItem o1, IFileSystemItem o2) {
                return Integer.compare(Integer.parseInt(o1.getName()), Integer.parseInt(o2.getName()));
            }
        });
        int i=0;
        for (IFileSystemItem f : temp) {
            res.add(new FileLayerMeta(f,fileSystem));
            i++;
            if(i==temp.size()-1){
                break;
            }
        }
        return res;
    }

    @Override
    public IResultLayerMeta getResultLayer() {
        // S layersDir = fileSystem.getItem(file+ fileSystem.getFolderSeparator()+"layers");
        //new File(file.getAbsolutePath() + File.pathSeparator + "layers");
        List<ILayerMeta> res = new ArrayList<>();
        List<S> temp = new ArrayList<>();
        if (!file.exists() || !file.isDirectory()) {
            throw new LayersFolderIsEmptyOrNotExistsException();
        }

        temp= fileSystem.listFiles(file);
        temp.removeIf(iFileSystemItem -> {
            if(!iFileSystemItem.isDirectory()){
                return true;
            }
            return false;
        });
        Collections.sort(temp, new Comparator<IFileSystemItem>() {
            @Override
            public int compare(IFileSystemItem o1, IFileSystemItem o2) {
                return Integer.compare(Integer.parseInt(o1.getName()), Integer.parseInt(o2.getName()));
            }
        });
        return new FileResultLayerMeta(temp.get(temp.size()-1),fileSystem);
    }

    @Override
    public ILayerMeta getLayerByID(int id) {
        for(ILayerMeta lm:getLayers()){
            if(lm.getID()==id){
                return lm;
            }
        }
        return null;
    }
}
