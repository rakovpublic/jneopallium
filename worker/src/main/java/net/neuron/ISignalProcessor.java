package net.neuron;

import net.signals.ISignal;

import java.util.List;

/***
 * Created by Rakovskyi Dmytro on 13.06.2018.
 * This class represents processor for signal
 */
public interface ISignalProcessor<S extends ISignal> {
    /***
    * @param input signal
    * @param neuron object
    * @return list of result signals
    * */
    <I extends ISignal> List<I> process(S input, INeuron neuron);

    /***
     * @return description
     * */
    String getDescription();

}
