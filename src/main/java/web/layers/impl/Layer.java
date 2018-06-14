package web.layers.impl;

import synchronizer.Context;
import synchronizer.IContext;
import web.layers.ILayer;
import web.neuron.IAxon;
import web.neuron.INeuron;
import web.neuron.impl.NeuronRunnerService;
import web.signals.ISignal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Rakovskyi Dmytro on 08.06.2018.
 */
public class Layer<N extends INeuron> implements ILayer<N> {
    private HashMap<String,N> map;
    private HashMap<IAxon,ISignal> results;
    private HashMap<String,List<ISignal> > input;

    public Layer(String layerId) {
        this.layerId = layerId;
        results=new HashMap<IAxon,ISignal>();
        map=new HashMap<String, N>();
        input=new HashMap<String,List<ISignal>>();
    }

    private String layerId;
    private IContext ctx= Context.getContext();
    @Override
    public void register(N neuron) {
        map.put(neuron.getId(),neuron);

    }
    @Override
    public void storeResult(ISignal signal, IAxon axon) {
        results.put(axon,signal);

    }

    @Override
    public void addInput(ISignal signal, String neuronId) {
        if(input.containsKey(neuronId)){
            input.get(neuronId).add(signal);
        }else{
            List<ISignal> list= new ArrayList<>();
            list.add(signal);
            input.put(neuronId,list);
        }
    }

    @Override
    public void process() {
        N neur;
        NeuronRunnerService ns= NeuronRunnerService.getService();
        for(String neuronId:map.keySet()){
            if(input.containsKey(neuronId)){
                neur=map.get(neuronId);
                neur.addSignals(input.get(neuronId));
                ns.addNeuron(neur);
            }
        }
        ns.process();
    }
    @Override
    public String getId() {
        return layerId;
    }
}
