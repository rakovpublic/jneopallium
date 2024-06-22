/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.signals.storage.http;

import com.rakovpublic.jneuropallium.worker.net.layers.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayersMeta;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.Neuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInputResolver;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.ISplitInput;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GRPCSplitInput implements ISplitInput {
    private static final Logger logger = LogManager.getLogger(GRPCSplitInput.class);
    private String nodeId;
    private IInputResolver inputResolver;
    private Long start;
    private Long end;
    private ILayersMeta layersMeta;
    private String discriminatorName;
    private Integer layerId;
    private Integer threads;

    public GRPCSplitInput(String nodeId, IInputResolver inputResolver, Long start, Long end, ILayersMeta layersMeta, String discriminatorName, Integer layerId, Integer threads) {
        this.nodeId = nodeId;
        this.inputResolver = inputResolver;
        this.start = start;
        this.end = end;
        this.layersMeta = layersMeta;
        this.discriminatorName = discriminatorName;
        this.layerId = layerId;
        this.threads = threads;
    }

    @Override
    public String getDiscriminatorName() {
        return discriminatorName;
    }

    @Override
    public void setDiscriminatorName(String name) {
        discriminatorName =name;
    }

    @Override
    public IInputResolver getInputResolver() {
        return inputResolver;
    }

    @Override
    public void saveResults(HashMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> signals) {
            inputResolver.getSignalPersistStorage().putSignals(signals);
    }

    @Override
    public void saveNeuron(INeuron neuron) {
        ILayerMeta meta = layersMeta.getLayerById(neuron.getLayer().getId());
        List<INeuron> neurons = meta.getNeurons();
        for(INeuron iNeuron: neurons){
            if(iNeuron.getId() == neuron.getId()){
                neurons.remove(iNeuron);
                break;
            }
        }
        meta.saveNeurons(neurons);
    }

    @Override
    public void setNodeIdentifier(String name) {
        nodeId=name;
    }

    @Override
    public ISplitInput getNewInstance() {
        return new GRPCSplitInput(nodeId, inputResolver, start, end, layersMeta, discriminatorName, layerId,threads) ;
    }

    @Override
    public List<? extends INeuron> getNeurons() {
        return  layersMeta.getLayerById(layerId).getNeurons(start,end);
    }

    @Override
    public String getNodeIdentifier() {
        return nodeId;
    }

    @Override
    public Long getStart() {
        return start;
    }

    @Override
    public Long getEnd() {
        return end;
    }

    @Override
    public void setStart(Long start) {
        this.start =start;
    }

    @Override
    public void setEnd(Long end) {
        this.end = end;
    }

    @Override
    public Integer getLayerId() {
        return layerId;
    }

    @Override
    public void setLayer(Integer layerId) {
        this.layerId =layerId;
    }

    @Override
    public void applyMeta(ILayersMeta layersMeta) {
        this.layersMeta = layersMeta;
    }

    @Override
    public Integer getThreads() {
        return threads;
    }
}
