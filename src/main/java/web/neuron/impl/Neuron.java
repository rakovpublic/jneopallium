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
    private Long neuronId;
    private List<ISignal> result;
    private ISignalChain processingChain;
    private List<IRule> rules;

    public Neuron(Long neuronId, ISignalChain processingChain) {
        rules= new ArrayList<>();
        this.neuronId = neuronId;
        isProcessed = false;
        signals = new ArrayList<>();
        result = new ArrayList<>();
        processorHashMap = new HashMap<>();
        mergerHashMap = new HashMap<>();
        this.processingChain = processingChain;
    }

    @Override
    public Boolean validate() {
        for(IRule r:rules){
            if(r.validate(this)==false){
                return false;
            }
        }
        return true;
    }

    @Override
    public void addRule(IRule rule) {
        rules.add(rule);
    }

    @Override
    public void addSignals(List<ISignal> signals) {
        signals.addAll(signals);
    }

    @Override
    public void processSignals() {
        HashMap<Class<? extends ISignal>, List<ISignal>> signalsMap = new HashMap<>();
        for (ISignal s : signals) {
            Class<? extends ISignal> cl = s.getCurrentClass();
            if (signalsMap.containsKey(cl)) {
                signalsMap.get(cl).add(s);
            } else {
                Class<? extends ISignal> clt = s.getCurrentClass();
                boolean done=true;
                while (getSupperClass(clt)!=ISignal.class&&getSupperClass(clt)!=Object.class){
                    if (signalsMap.containsKey(clt)){
                        signalsMap.get(clt).add(s);
                        done=false;
                    }
                }
                if(done) {
                    List<ISignal> ll = new ArrayList<>();
                    ll.add(s);
                    signalsMap.put(cl, ll);
                }
            }
        }
        for (Class<? extends ISignal> cl : processingChain.getProcessingChain()) {
            if (signalsMap.containsKey(cl)) {
                ISignalMerger signalMerger = mergerHashMap.get(cl);
                ISignalProcessor signalProcessor = processorHashMap.get(cl);
                if (signalMerger == null) {
                    throw new CannotFindSignalMergerException("Cannot find signal merger for signal class" + cl.getCanonicalName() + " in neuron id" + this.neuronId);
                }
                if (signalProcessor == null) {
                    throw new CannotFindSignalProcessorException("Cannot find signal processor for signal class" + cl.getCanonicalName() + " in neuron id" + this.neuronId);
                }
                ISignal inS = signalMerger.mergeSignals(signalsMap.get(cl));
                result.addAll(signalProcessor.process(inS, this));
            }
        }

    }

    @Override
    public void reconfigure() {

    }

    @Override
    public void setAxon(IAxon axon) {
        this.axon = axon;
    }

    @Override
    public <S extends ISignal> void addSignalProcessor(Class<S> clazz, ISignalProcessor<S> processor) {
        processorHashMap.put(clazz, processor);
    }

    @Override
    public <S extends ISignal> void addSignalMerger(Class<S> clazz, ISignalMerger<S> merger) {
        mergerHashMap.put(clazz, merger);
    }

    @Override
    public <S extends ISignal> void removeSignalProcessor(Class<S> clazz) {
        processorHashMap.remove(clazz);
        this.removeSignalMerger(clazz);
    }

    @Override
    public <S extends ISignal> void removeSignalMerger(Class<S> clazz) {
        mergerHashMap.remove(clazz);
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
    public Long getId() {
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

    @Override
    public void setProcessingChain(ISignalChain chain) {
        this.processingChain = chain;
    }


    private Class<?> getSupperClass(Class<? extends ISignal> clazz){
        return clazz.getSuperclass();
    }
}
