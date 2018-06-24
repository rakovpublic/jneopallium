package web.layers;

/**
 * Created by Rakovskyi Dmytro on 27.10.2017.
 */

import web.neuron.INeuron;
import web.signals.ISignal;
import web.storages.IInputMeta;

/**
 * listener for neurons
 */
public interface ILayer {
    void register(INeuron neuron);

    void addInput(ISignal signal, Long neuronId);

    void process();

    int getId();

    Boolean isProcessed();

    void dumpResult(IInputMeta meta);

    String toJSON();

}
