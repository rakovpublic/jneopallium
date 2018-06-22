package web.storages;

import web.neuron.INeuron;

import java.util.List;

public interface ILayerMeta extends IStorageMeta {
    List<INeuronMeta<? extends INeuron>> getNeurons();

}
