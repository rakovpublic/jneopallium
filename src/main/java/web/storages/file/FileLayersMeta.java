package web.storages.file;

import exceptions.LayersFolderIsEmptyOrNotExistsException;
import web.storages.ILayerMeta;
import web.storages.ILayersMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FileLayersMeta implements ILayersMeta {
    private File file;

    public FileLayersMeta(File file) {
        this.file = file;
    }

    @Override
    public List<ILayerMeta> getLayers() {
        File layersDir= new File(file.getAbsolutePath()+File.pathSeparator+"layers");
        List<ILayerMeta> res= new ArrayList<>();
        List<File> temp= new ArrayList<>();
        if (!layersDir.exists()||!layersDir.isDirectory()){
            throw new LayersFolderIsEmptyOrNotExistsException();
        }
        for(File l:layersDir.listFiles()){
            if(!l.isDirectory()){
                temp.add(l);
            }
        }
        Collections.sort(temp, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return Integer.compare(Integer.parseInt(o1.getName()),Integer.parseInt(o2.getName()));
            }
        });
        for(File f:temp){
            res.add(new FileLayerMeta(f));
        }
        return res;
    }
}
