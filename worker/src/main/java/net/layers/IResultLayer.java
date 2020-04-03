package net.layers;

import net.layers.impl.SimpleResultWrapper;
import net.signals.IResultSignal;

/***
 * Created by Rakovskyi Dmytro .
 *
 * Represents neuron result layer and encapsulate result interpretation logic
 *
 *
 */
public interface IResultLayer<K> extends ILayer {
    /*
     * Transfer result layer to result
     *
     * */
    IResult<K> interpretResult();


}
