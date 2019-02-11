package synchronizer;

import web.neuron.INeuron;
import web.signals.ISignal;

import java.util.List;

/**
 * Created by Rakovskyi Dmytro on 27.10.2017.
 */
public interface ISynchronizer {

    IContext getContext(int nodeId);
    void syncSignal(ISignal signal,int layerId, long neuronId);
    boolean isLayerProcessed(int layerId);
    void syncNeurons(List<? extends INeuron> neurons,int layerId);
    void removeNeuron(int layerId, long neuronId);
    void updateNeuron(INeuron neuron, int layerId);
    void addNeuron(INeuron neuron,long layerId);


}
