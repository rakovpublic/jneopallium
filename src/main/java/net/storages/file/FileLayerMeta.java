package net.storages.file;

import net.neuron.INeuron;
import net.storages.ILayerMeta;
import net.storages.INeuronMeta;
import net.storages.filesystem.IFileSystem;
import net.storages.filesystem.IFileSystemItem;

import java.util.Collection;
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
    public void saveNeurons(Collection<? extends INeuron> neuronMetas) {

    }


    @Override
    public void dumpLayer() {

    }
}
