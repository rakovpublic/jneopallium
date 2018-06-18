package web.layers;

/**
 * Created by Rakovskyi Dmytro on 27.10.2017.
 */

import web.neuron.IAxon;
import web.neuron.INeuron;
import web.signals.ISignal;

/**
 *
 * listener for neurons
 * */
public interface ILayer<N extends INeuron> {
    void register(N neuron);
    void addInput(ISignal signal,String neuronId);
    void process();
    String getId();
    Boolean isProcessed();
    void dumpResult();

}
