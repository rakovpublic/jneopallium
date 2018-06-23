package web.neuron.impl;

import web.neuron.*;
import web.signals.ISignal;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

 public class Neuron implements INeuron {
    private List<ISignal> signals;
    private Boolean isProcessed;
    private IAxon axon;
    private HashMap<Class<? extends ISignal>, ISignalProcessor> processorHashMap;
     private HashMap<Class<? extends ISignal>, ISignalMerger> mergerHashMap;
     private String neuronId;


     @Override
    public void addSignals(List<ISignal> signals) {
        signals.addAll(signals);
    }

    @Override
    public void processSignals() {
         HashMap<Class<? extends ISignal>, List<ISignal>> signalsMap=new HashMap<>();
         for(ISignal s:signals){
             Class<? extends ISignal> cl=s.getCurrentClass();
             if(signalsMap.containsKey(cl)){
                signalsMap.get(cl).add(s);
             }else{
                 List<ISignal> ll= new ArrayList<>();
                 ll.add(s);
                 signalsMap.put(cl,ll);
             }
         }

    }

    @Override
    public void reconfigure() {

    }

    @Override
    public void setAxon(IAxon axon) {

    }

    @Override
    public <S extends ISignal> void addSignalProcessor(Class<S> clazz, ISignalProcessor<S> processor) {
        processorHashMap.put(clazz,processor);
    }

     @Override
     public <S extends ISignal> void addSignalMerger(Class<S> clazz, ISignalMerger<S> merger) {
         mergerHashMap.put(clazz,merger);
     }

     @Override
    public HashMap<ISignal, List<INConnection>> getResult() {
        return null;
    }

    @Override
    public Boolean hasResult() {
        return this.isProcessed;
    }

    @Override
    public String getId() {
        return this.neuronId;
    }

    @Override
    public String toJSON() {
        return null;
    }
}
