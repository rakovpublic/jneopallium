package com.rakovpublic.jneuropallium.worker.net.storages.structimpl;

import com.rakovpublic.jneuropallium.worker.net.layers.IInputResolver;
import com.rakovpublic.jneuropallium.worker.net.layers.LayerMove;
import com.rakovpublic.jneuropallium.worker.net.storages.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.net.storages.ILayersMeta;
import com.rakovpublic.jneuropallium.worker.net.storages.IResultLayerMeta;
import com.rakovpublic.jneuropallium.worker.net.storages.IStructMeta;
import com.rakovpublic.jneuropallium.worker.net.study.ILearningRequest;
import com.rakovpublic.jneuropallium.worker.neuron.INeuron;

import java.util.List;
import java.util.TreeMap;

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
    public void removeLayer(Integer layerId) {
        TreeMap<Integer,ILayerMeta> layers = new TreeMap<>();
        for(ILayerMeta layerMeta : layersMeta.getLayers()){
            layers.put(layerMeta.getID(),layerMeta);
        }
        Integer nextLayerId = layers.higherKey(layerId);
        Integer prevLayerId = layers.ceilingKey(layerId);
        LayerMove layerMove = new LayerMove(layerId,nextLayerId,prevLayerId);
        for(ILayerMeta layerMeta : layersMeta.getLayers()){
            layerMeta.addLayerMove(layerMove);
        }
        ILayerMeta layerToRemove = layers.get(layerId);
        layersMeta.removeLayer(layerToRemove);
    }


}
