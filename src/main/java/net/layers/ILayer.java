package net.layers;

/**
 * Created by Rakovskyi Dmytro on 27.10.2017.
 */

import net.neuron.INeuron;
import net.neuron.IRule;
import net.signals.ISignal;
import net.storages.IInputMeta;
import net.storages.ILayerMeta;

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

    void dumpNeurons(ILayerMeta layerMeta);

    HashMap<Integer, HashMap<Long, List<ISignal>>> getResults();

    String toJSON();

}
