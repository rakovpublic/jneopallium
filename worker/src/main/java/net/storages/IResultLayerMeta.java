package net.storages;

import net.neuron.INeuron;
import net.neuron.IResultNeuron;

import java.util.Collection;
import java.util.List;

public interface IResultLayerMeta extends ILayerMeta {
    List<? extends IResultNeuron> getNeurons();

}
