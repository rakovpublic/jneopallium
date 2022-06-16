package com.rakovpublic.jneuropallium.worker.net.storages.structimpl;

import com.rakovpublic.jneuropallium.worker.net.layers.IInputResolver;
import com.rakovpublic.jneuropallium.worker.net.layers.LayerMove;
import com.rakovpublic.jneuropallium.worker.net.storages.*;
import com.rakovpublic.jneuropallium.worker.net.study.ILearningRequest;
import com.rakovpublic.jneuropallium.worker.neuron.INeuron;

import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

/*

 * TODO: refactor for usage  Input strategy
 **/
public class StructMeta implements IStructMeta {



    private IInputResolver inputResolver;
    private ILayersMeta layersMeta;


    public StructMeta(IInputResolver hiddenInputMeta, ILayersMeta layersMeta) {

        this.inputResolver = hiddenInputMeta;
        this.layersMeta = layersMeta;
    }

    void init() {
    }

    @Override
    public List<ILayerMeta> getLayers() {
        return layersMeta.getLayers();
    }


    @Override
    public IInputResolver getInputResolver() {

        return inputResolver;
    }


    @Override
    public void learn(List<ILearningRequest> requests) {
        //TODO:write optimization now work for small local config
        for (ILearningRequest request : requests) {
            ILayerMeta lm = layersMeta.getLayerByID(request.getLayerId());
            INeuron ner = lm.getNeuronByID(request.getNeuronId());
            ner.getAxon().resetConnection(request.getNewConnections());
        }
        for (ILayerMeta meta : layersMeta.getLayers()) {
            meta.dumpLayer();
        }
        layersMeta.getResultLayer().dumpLayer();
    }

    @Override
    public IResultLayerMeta getResultLayer() {
        return layersMeta.getResultLayer();
    }

    @Override
    public void removeLayer(Integer layerId, ReconnectStrategy reconnectStrategy) {

        HashMap<Integer,HashMap<Long,HashMap<Integer, List<Long>>>> updateMap = reconnectStrategy.getNewConnections(layersMeta,layerId);

        for(Integer layersToFix: updateMap.keySet()){
            ILayerMeta layerMeta = layersMeta.getLayerByID(layersToFix);
            layerMeta.addLayerMove( new LayerMove(updateMap.get(layersToFix),layerId));
        }
        ILayerMeta layerToRemove = layersMeta.getLayerByID(layerId);
        layersMeta.removeLayer(layerToRemove);
    }


}
