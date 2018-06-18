package web.layers;

import web.neuron.INeuron;

public interface IResultLayer<N extends INeuron,K> extends ILayer<N>{
    K interpretResult();

}
