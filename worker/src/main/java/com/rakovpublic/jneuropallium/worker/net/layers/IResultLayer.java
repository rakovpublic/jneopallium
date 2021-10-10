package com.rakovpublic.jneuropallium.worker.net.layers;

import java.util.List;

/***
 * Created by Rakovskyi Dmytro .
 *
 * Represents neuron result layer and encapsulate result interpretation logic
 *
 *
 */
public interface IResultLayer extends ILayer {
    /*
     * Transfer result layer to result
     *
     * */
    List<IResult> interpretResult();


}
