package web.storages.file;

import web.neuron.INeuron;
import web.storages.ILayerMeta;
import web.storages.INeuronMeta;
import web.storages.filesystem.IFileSystem;
import web.storages.filesystem.IFileSystemItem;

import java.io.File;
import java.util.List;

public class FileLayerMeta<S extends IFileSystemItem> implements ILayerMeta {
    private S file;
    private IFileSystem<S> fileSystem;
    FileLayerMeta(S file, IFileSystem<S> fs) {
        this.file = file;
        this.fileSystem=fs;
    }

    @Override
    public List<INeuronMeta<? extends INeuron>> getNeurons() {
        return null;
    }

    @Override
    public void saveNeurons(List<INeuronMeta<? extends INeuron>> neuronMetas) {

    }

    @Override
    public void dumpLayer() {

    }
}
