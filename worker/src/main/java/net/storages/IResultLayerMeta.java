package net.storages;

import net.neuron.IResultNeuron;

import java.util.List;

public interface IResultLayerMeta extends ILayerMeta {
    List<? extends IResultNeuron> getNeurons();

}
