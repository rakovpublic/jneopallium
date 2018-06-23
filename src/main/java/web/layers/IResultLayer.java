package web.layers;

import web.neuron.INeuron;

public interface IResultLayer<K> extends ILayer{
    K interpretResult();

}
