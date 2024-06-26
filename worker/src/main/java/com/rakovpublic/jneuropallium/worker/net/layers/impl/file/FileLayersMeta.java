package com.rakovpublic.jneuropallium.worker.net.layers.impl.file;

import com.google.gson.internal.LinkedTreeMap;
import com.rakovpublic.jneuropallium.worker.exceptions.LayersFolderIsEmptyOrNotExistsException;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayersMeta;
import com.rakovpublic.jneuropallium.worker.net.layers.IResultLayerMeta;
import com.rakovpublic.jneuropallium.worker.net.storages.filesystem.IStorage;
import com.rakovpublic.jneuropallium.worker.net.storages.filesystem.IStorageItem;

import java.util.*;
import java.util.stream.Collectors;


public class FileLayersMeta<S extends IStorageItem> implements ILayersMeta {
    private S file;
    private final IStorage<S> fileSystem;
    private final LinkedTreeMap<Integer, ILayerMeta> layers;
    private final LinkedList<ILayerMeta> layerMetas;


    public FileLayersMeta(S file, IStorage<S> fs) {
        this.fileSystem = fs;
        this.file = file;
        layers = new LinkedTreeMap<>();
        layerMetas = new LinkedList<>();
        getLayers();
    }


    @Override
    public void setRootPath(String path) {
        file = fileSystem.getItem(path);
    }

    @Override
    public List<ILayerMeta> getLayers() {
        // S layersDir = fileSystem.getItem(file+ fileSystem.getFolderSeparator()+"layers");
        //new File(file.getAbsolutePath() + File.pathSeparator + "layers");
        if (layers.isEmpty() || layers.size() == 1) {
            List<ILayerMeta> res = new ArrayList<>();
            List<S> temp = new ArrayList<>();
            if (!file.exists() || !file.isDirectory()) {
                throw new LayersFolderIsEmptyOrNotExistsException();
            }

            temp = fileSystem.listFiles(file);
            Collections.sort(temp, new Comparator<IStorageItem>() {
                @Override
                public int compare(IStorageItem o1, IStorageItem o2) {
                    return Integer.compare(Integer.parseInt(o1.getName()), Integer.parseInt(o2.getName()));
                }
            });
            int i = 0;
            for (IStorageItem f : temp) {
                ILayerMeta layerMeta = new FileLayerMeta(f, fileSystem);
                layers.put(layerMeta.getID(), layerMeta);
                res.add(layerMeta);
                i++;
                if (i == temp.size() - 1) {
                    break;
                }
            }
            layerMetas.clear();
            layerMetas.addAll(layers.values());
            return res;
        } else {
            return layers.values().stream().collect(Collectors.toList());
        }

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

        temp = fileSystem.listFiles(file);
        temp.removeIf(iFileSystemItem -> {
            return iFileSystemItem.isDirectory();
        });
        Collections.sort(temp, new Comparator<IStorageItem>() {
            @Override
            public int compare(IStorageItem o1, IStorageItem o2) {
                return Integer.compare(Integer.parseInt(o1.getName()), Integer.parseInt(o2.getName()));
            }
        });
        return new FileResultLayerMeta(temp.get(temp.size() - 1), fileSystem);
    }

    @Override
    public ILayerMeta getLayerByPosition(int id) {
        if (layerMetas.contains(id)) {
            return layerMetas.get(id);
        } else {
            return layers.get(id);
        }
    }

    @Override
    public ILayerMeta getLayerById(int id) {
        for (ILayerMeta layer : layerMetas) {
            if (layer.getID() == id) {
                return layer;
            }
        }
        if (layers.containsKey(id)) {
            return layers.get(id);
        }
        return null;
    }


    @Override
    public void addLayerMeta(ILayerMeta layerMeta) {
        layers.put(layerMeta.getID(), layerMeta);

    }

    @Override
    public void addLayerMeta(ILayerMeta layerMeta, int position) {
        layers.put(position, layerMeta);
    }

    @Override
    public void removeLayer(ILayerMeta layerMeta) {
        layers.remove(layerMeta);
    }
}
