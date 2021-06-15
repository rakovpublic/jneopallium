package com.rakovpublic.jneuropallium.worker.net.storages.structimpl;

import com.rakovpublic.jneuropallium.worker.net.layers.IInputResolver;
import com.rakovpublic.jneuropallium.worker.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.*;
import com.rakovpublic.jneuropallium.worker.net.study.IStudyingRequest;

import java.util.HashMap;
import java.util.List;
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

    void init(){}

    @Override
    public List<ILayerMeta> getLayers() {
        return layersMeta.getLayers();
    }


    @Override
    public IInputResolver getInputResolver() {

        return inputResolver;
    }


    @Override
    public void study(List<IStudyingRequest> requests) {
        //TODO:write optimization now work for small local config
        for (IStudyingRequest request : requests) {
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


}
