package net.neuron;

import net.signals.ISignal;

/**
 * Created by Rakovskyi Dmytro on 14.06.2018.
 */
public interface IWeight<S extends ISignal, C extends ISignal> {
    S process(S signal);
    void changeWeight(C signal);
    Class<S> getSignalClass();
}
