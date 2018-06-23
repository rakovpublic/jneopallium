package web.storages.file;

import web.neuron.INeuron;
import web.storages.ILayerMeta;
import web.storages.INeuronMeta;

import java.io.File;
import java.util.List;

public class FileLayerMeta implements ILayerMeta {
    private File file;

    public FileLayerMeta(File file) {
        this.file = file;
    }

    @Override
    public List<INeuronMeta<? extends INeuron>> getNeurons() {
        return null;
    }

    @Override
    public void saveNeurons(List<INeuronMeta<? extends INeuron>> neuronMetas) {

    }
}
