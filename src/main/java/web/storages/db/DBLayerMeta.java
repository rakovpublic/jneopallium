package web.storages.db;

import web.neuron.INeuron;
import web.storages.ILayerMeta;
import web.storages.INeuronMeta;

import java.util.List;

public class DBLayerMeta implements ILayerMeta {


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
