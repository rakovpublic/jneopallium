package web.layers;

/**
 * Created by Rakovskyi Dmytro on 27.10.2017.
 */

import web.neuron.INeuron;
import web.neuron.IRule;
import web.signals.ISignal;
import web.storages.IInputMeta;

import java.util.HashMap;
import java.util.List;

/**
 * listener for neurons
 */
public interface ILayer {
    long getLayerSize();
    Boolean validateGlobal();
    Boolean validateLocal();
    void addGlobalRule(IRule rule);

    void register(INeuron neuron);

    void addInput(ISignal signal, Long neuronId);

    void process();

    int getId();

    Boolean isProcessed();

    void dumpResult(IInputMeta meta);

    void dumpNeurons();

    HashMap<Integer, HashMap<Long, List<ISignal>>> getResults();

    String toJSON();

}
