package net.storages.db;

import net.neuron.INeuron;
import net.storages.ILayerMeta;
import net.storages.INeuronMeta;

import java.util.Collection;
import java.util.List;

public class DBLayerMeta implements ILayerMeta {


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
