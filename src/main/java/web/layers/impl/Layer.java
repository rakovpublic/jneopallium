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
    private HashMap<String,List<ISignal> > input;
    private Boolean isProcessed;
    private List<N> notProcessed;

    public Layer(String layerId) {
        isProcessed=false;
        notProcessed=new ArrayList<N>();
        this.layerId = layerId;
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

    @Override
    public Boolean isProcessed() {

        if(!isProcessed&&notProcessed.size()==0){
            isProcessed=true;
            for(N ner:map.values()){
                if(!ner.hasResult()){
                    notProcessed.add(ner);
                    isProcessed=false;
                }
            }
        }else {

            for(N ner:notProcessed){
                if(ner.hasResult()){
                    notProcessed.remove(ner);
                }
            }
            if(notProcessed.size()==0){
                isProcessed=true;
            }
        }
        return isProcessed;
    }

    @Override
    public void dumpResult() {

    }
}
