package net.layers;

/**
 * Created by Rakovskyi Dmytro on 27.10.2017.
 *
 * Represents neuron layer and contains neuron and signals.
 * Also contains neuron processing logic, result storing and neuron settings storing.
 *
 *
 */

import net.neuron.INeuron;
import net.neuron.IRule;
import net.signals.ISignal;
import net.storages.IInputMeta;
import net.storages.ILayerMeta;

import java.util.HashMap;
import java.util.List;

public interface ILayer {

    /*
    * @return layer size
    * **/
    long getLayerSize();
    /*
    * Validate neurons using validation rules set up on layers level.
    * @return true if all neurons is valid
    * */
    Boolean validateGlobal();

    /*
    * Validate neurons using validation rules from neuron object.
    * @return true if all neurons is valid
    * */
    Boolean validateLocal();

    /*
    * Add validation rule on layer level
    * @param validation rule
    * */
    void addGlobalRule(IRule rule);

    /*
    * register neuron in the layer
    * @param neuron
    * */
    void register(INeuron neuron);

    /*
    * add input to layer
    * @param signal
    * @param neuron id
    * */
    void addInput(ISignal signal, Long neuronId);

    /*
    * this method launch the layer processing
    * */
    void process();

    /*
    * @return layer id
    * **/
    int getId();

    /*
    * Check if all neurons processed
    * @return true if all neurons processed
    * **/
    Boolean isProcessed();

    /*
    * Store signal which obtained after neuron processing.
    * @param meta data to for input storage
    * **/
    void dumpResult(IInputMeta meta);

    /*
    * Store neurons after processing, because the state of neuron could be changed.
    * @param meta data for layer storage
    * */
    void dumpNeurons(ILayerMeta layerMeta);

    /*
    * Obtain result data structure which consists layer id, neuron id and list of signals.
    * @return hash map with integer key - layer id , value - hash map with long key - neuron id, list value  - signals
    * */
    HashMap<Integer, HashMap<Long, List<ISignal>>> getResults();

    /*
    * serialize layer to json
    * @return string json
    * **/
    String toJSON();

}
