package web.neuron;

import web.signals.ISignal;

/**
 * Created by Rakovskyi Dmytro on 14.06.2018.
 */
public interface IWeight<S extends ISignal> {
    S process(S signal);
    Class<S> getSignalClass();
}
