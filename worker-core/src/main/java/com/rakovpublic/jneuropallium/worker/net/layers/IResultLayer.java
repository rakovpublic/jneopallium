package com.rakovpublic.jneuropallium.worker.net.layers;

import com.rakovpublic.jneuropallium.worker.net.neuron.IResultNeuron;

import java.util.List;

/***
 * Created by Rakovskyi Dmytro .
 *
 * Represents neuron result layer and encapsulate result interpretation logic
 *
 *
 */
public interface IResultLayer<N extends IResultNeuron> extends ILayer<N> {
    /*
     * Transfer result layer to result
     *
     * */
    List<IResult> interpretResult();


}
