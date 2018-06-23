package web.neuron.impl;

import exceptions.CannotFindSignalMergerException;
import exceptions.CannotFindSignalProcessorException;
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
     private List<ISignal> result;


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
         for(Class<? extends ISignal> cl:signalsMap.keySet()){
             ISignalMerger signalMerger=mergerHashMap.get(cl);
             ISignalProcessor signalProcessor=processorHashMap.get(cl);
             if(signalMerger==null){
                 throw new CannotFindSignalMergerException("Cannot find signal merger for signal class"+cl.getCanonicalName()+" in neuron id"+this.neuronId);
             }
             if(signalProcessor==null){
                 throw new CannotFindSignalProcessorException( "Cannot find signal processor for signal class"+cl.getCanonicalName()+" in neuron id"+this.neuronId);
             }
             ISignal inS=signalMerger.mergeSignals(signalsMap.get(cl));
             result.add(signalProcessor.process(inS));
         }

    }

    @Override
    public void reconfigure() {

    }

    @Override
    public void setAxon(IAxon axon) {
        this.axon=axon;
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
     public List<ISignal> getResult() {
         return this.result;
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
     public IAxon getAxon() {
         return this.axon;
     }

     @Override
    public String toJSON() {
        return null;
    }
}
