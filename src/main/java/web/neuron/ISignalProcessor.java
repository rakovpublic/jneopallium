package web.neuron;

import web.signals.ISignal;

/**
 * Created by Rakovskyi Dmytro on 13.06.2018.
 */
public interface ISignalProcessor<S extends ISignal> {
    <I extends ISignal> I process(S input, INeuron neuron);
    String getDescription();

}
