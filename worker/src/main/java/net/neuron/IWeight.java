package net.neuron;

import net.signals.ISignal;

/***
 * Created by Rakovskyi Dmytro on 14.06.2018.
 * This class represents weight/value of connection between neurons.
 */
public interface IWeight<S extends ISignal, C extends ISignal> {

    /**
    * Process signal and change it due to the weight/value
    * @param signal
    * @return signal
    * **/
    S process(S signal);

    /**
    * Change the weight due to the change signal.
    * @param signal change signal
    * **/
    void changeWeight(C signal);

    /**
    * @return signal class
    * **/
    Class<S> getSignalClass();
}