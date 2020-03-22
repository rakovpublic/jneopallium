package sample;

import net.neuron.INeuron;
import net.neuron.IResultNeuron;
import net.storages.IStructMeta;
import net.study.IStudyingAlgorithm;
import net.study.IStudyingRequest;

import java.util.List;

public class SimpleStudyingAlgo implements IStudyingAlgorithm {
    @Override
    public List<IStudyingRequest> study(IStructMeta structMeta,Long neuronId) {
        INeuron rNeuron=structMeta.getResultLayer().getNeuronByID(neuronId);
        return null;
    }
}
