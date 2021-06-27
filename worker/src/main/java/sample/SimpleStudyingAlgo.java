package sample;

import com.rakovpublic.jneuropallium.worker.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.net.storages.IStructMeta;
import com.rakovpublic.jneuropallium.worker.net.study.IDirectStudyingAlgorithm;
import com.rakovpublic.jneuropallium.worker.net.study.IStudyingRequest;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class SimpleStudyingAlgo implements IDirectStudyingAlgorithm {
    @Override
    public List<IStudyingRequest> study(IStructMeta structMeta,Long neuronId) {
        List<IStudyingRequest> result= new LinkedList<>();
        INeuron rNeuron=structMeta.getResultLayer().getNeuronByID(neuronId);
        int layerId =structMeta.getResultLayer().getID();
        List<ISignal> resultSignals = structMeta.getInputResolver().getSignalPersistStorage().getLayerSignals(layerId).get(neuronId);
        for(ISignal s:resultSignals){
            rebuildConnection(s.getSourceNeuronId(),s.getSourceLayerId(),structMeta,neuronId,result);
        }
        return result;
    }
    //TODO: refactor to remove recursion invoke to avoid stack overflow
    private void rebuildConnection(Long neuronId, int layerId, IStructMeta structMeta, Long sourceNeuron,  List<IStudyingRequest> result){
        List<ISignal> resultSignals = structMeta.getInputResolver().getSignalPersistStorage().getLayerSignals(layerId).get(neuronId);
        Optional<ILayerMeta> layer = structMeta.getLayers().stream().filter(l->l.getID()==layerId).findFirst();
        if(!layer.isPresent()){
            System.out.println("cannot get layer meta");
            //TODO: add logger
        }
        layer.ifPresent(l->{l.getNeuronByID(neuronId).getAxon().changeAllWeightsForNeuron(layerId+1,sourceNeuron, new SimpleChangeWeightSignal(0.5d,layerId,neuronId));
            result.add(new SimpleStudyingRequest(layerId,neuronId,l.getNeuronByID(neuronId).getAxon().getConnectionMap()));});
        for(ISignal s:resultSignals){
            rebuildConnection(s.getSourceNeuronId(),s.getSourceLayerId(),structMeta,neuronId,result);
        }
    }
}
