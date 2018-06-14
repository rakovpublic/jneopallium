package web.neuron;

import web.signals.ISignal;

import java.util.HashMap;

/**
 * Created by Rakovskyi Dmytro on 13.06.2018.
 */
public interface ISignalProcessor<S extends ISignal> {
    <I extends ISignal> I proccess(S input);

}
