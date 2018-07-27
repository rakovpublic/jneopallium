package web.neuron;

import web.signals.ISignal;

/**
 * Created by Rakovskyi Dmytro on 14.06.2018.
 */
public interface IWeight<S extends ISignal, C extends IChangeSignal> {
    S process(S signal);
    void changeWeight(ISignal signal);
    Class<S> getSignalClass();
}
