package web.neuron;

import web.signals.ISignal;

import java.util.List;

/**
 * Created by Rakovskyi Dmytro on 13.06.2018.
 */
public interface ISignalProcessor<S extends ISignal> {
    <I extends ISignal> List<I> process(S input, INeuron neuron);
    String getDescription();

}
