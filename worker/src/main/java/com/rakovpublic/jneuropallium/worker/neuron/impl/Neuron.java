package com.rakovpublic.jneuropallium.worker.neuron.impl;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.rakovpublic.jneuropallium.worker.exceptions.CannotFindSignalMergerException;
import com.rakovpublic.jneuropallium.worker.exceptions.CannotFindSignalProcessorException;
import com.rakovpublic.jneuropallium.worker.neuron.*;
import com.rakovpublic.jneuropallium.worker.net.signals.IChangingSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public  class Neuron implements INeuron {
    private List<ISignal> signals;
    private Boolean isProcessed;
    private IAxon axon;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private HashMap<Class<? extends ISignal>, ISignalProcessor> processorMap;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private HashMap<Class<? extends ISignal>, ISignalMerger> mergerMap;
    private Long neuronId;
    protected List<ISignal> result;
    protected  ISignalChain signalChain;
    private List<IRule> rules;
    private Class<?extends INeuron> currentNeuronClass;
    private Boolean changed;
    private List<CreateNeuronRequest> createNeuronRequests = new ArrayList<>();
    private Boolean onDelete;
@Override
    public HashMap<String, Long> getCyclingNeuronInputMapping() {
        return cyclingNeuronInputMapping;
    }
@Override
    public void setCyclingNeuronInputMapping(HashMap<String, Long> cyclingNeuronInputMapping) {
        this.cyclingNeuronInputMapping = cyclingNeuronInputMapping;
    }
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    protected HashMap<String,Long> cyclingNeuronInputMapping;



    public Neuron() {
        rules = new ArrayList<>();
        isProcessed = false;
        signals = new ArrayList<>();
        result = new ArrayList<>();
        processorMap = new HashMap<>();
        mergerMap = new HashMap<>();
        currentNeuronClass=Neuron.class;
    }

    public Neuron(Long neuronId, ISignalChain processingChain) {
        rules = new ArrayList<>();
        this.neuronId = neuronId;
        isProcessed = false;
        signals = new ArrayList<>();
        result = new ArrayList<>();
        processorMap = new HashMap<>();
        mergerMap = new HashMap<>();
        this.signalChain = processingChain;
    }

    @Override
    public ISignalChain getSignalChain() {
        return signalChain;
    }

    @Override
    public Map<Class<? extends ISignal>, ISignalProcessor> getProcessorMap() {
        return processorMap;
    }

    @Override
    public Map<Class<? extends ISignal>, ISignalMerger> getMergerMap() {
        return mergerMap;
    }

    @Override
    public void setId(Long id) {
        this.neuronId = id;
    }

    @Override
    public Boolean validate() {
        for (IRule r : rules) {
            if (!r.validate(this)) {
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
        this.signals.addAll(signals);
    }

    @Override
    public void processSignals() {
        HashMap<Class<? extends ISignal>, List<ISignal>> signalsMap = new HashMap<>();
        for (ISignal s : signals) {

            Class<? extends ISignal> cl = s.getClass();
            if (signalsMap.containsKey(cl)) {
                signalsMap.get(cl).add(s);
            } else {
                List<ISignal> ll = new ArrayList<>();
                ll.add(s);
                signalsMap.put(cl, ll);
            }
        }
        for (Class<? extends ISignal> cl : this.getSignalChain().getProcessingChain()) {
            for (Class<? extends ISignal> cls : signalsMap.keySet()) {
                for(ISignal signal : signalsMap.get(cls)){
                    if(signal instanceof IChangingSignal){
                        IChangingSignal cs= (IChangingSignal) signal;
                        if(cs.canProcess(this.getClass())){
                            cs.changeNeuron(this);
                        }
                    }
                }
                if (cl.equals(cls)) {
                    ISignalMerger signalMerger = this.getMergerMap().get(cl);
                    ISignalProcessor signalProcessor = this.getProcessorMap().get(cl);
                    if (signalProcessor == null) {
                        throw new CannotFindSignalProcessorException("Cannot find signal processor for signal class" + cl.getCanonicalName() + " in neuron id" + this.neuronId);
                    }
                    if (signalMerger != null && (signalProcessor.hasMerger() != null ? signalProcessor.hasMerger() : true)) {
                        ISignal inS = signalMerger.mergeSignals(signalsMap.get(cl));
                        result.addAll(signalProcessor.process(inS, this));
                    } else if (signalProcessor.hasMerger() == null || !signalProcessor.hasMerger()) {
                        for (ISignal s : signalsMap.get(cl)) {
                            result.addAll(signalProcessor.process(s, this));
                        }
                    } else {
                        throw new CannotFindSignalMergerException("Cannot find signal merger for signal class" + cl.getCanonicalName() + " in neuron id" + this.neuronId);
                    }
                } else if (!this.getSignalChain().getProcessingChain().contains(cls)) {
                    ISignal s = signalsMap.get(cls).get(0);
                    Class<?> clst = cls;
                    boolean done = true;
                    while (getSupperClass(clst) != ISignal.class && getSupperClass(clst) != Object.class && s.canUseProcessorForParent() && done) {
                        if (cl.equals(clst)) {
                            ISignalMerger signalMerger = this.getMergerMap().get(cl);
                            ISignalProcessor signalProcessor = this.getProcessorMap().get(cl);
                            if (signalProcessor == null) {
                                throw new CannotFindSignalProcessorException("Cannot find signal processor for signal class" + cl.getCanonicalName() + " in neuron id" + this.neuronId);
                            }
                            if (signalMerger != null && (signalProcessor.hasMerger() != null ? signalProcessor.hasMerger() : true)) {
                                ISignal inS = signalMerger.mergeSignals(signalsMap.get(cl));
                                result.addAll(signalProcessor.process(inS, this));
                            } else if (signalProcessor.hasMerger() == null || !signalProcessor.hasMerger()) {
                                for (ISignal st : signalsMap.get(cl)) {
                                    result.addAll(signalProcessor.process(st, this));
                                }
                            } else {
                                throw new CannotFindSignalMergerException("Cannot find signal merger for signal class" + cl.getCanonicalName() + " in neuron id" + this.neuronId);
                            }
                            done = false;
                        }
                        clst = getSupperClass(clst);
                    }
                }
            }
        }
        this.isProcessed = true;

    }

    @Override
    public void setAxon(IAxon axon) {
        this.axon = axon;
    }

    @Override
    public <S extends ISignal,N extends INeuron> void addSignalProcessor(Class<S> clazz, ISignalProcessor<S,N> processor) {
        processorMap.put(clazz, processor);
    }

    @Override
    public <S extends ISignal> void addSignalMerger(Class<S> clazz, ISignalMerger<S> merger) {
        mergerMap.put(clazz, merger);
    }

    @Override
    public <S extends ISignal> void removeSignalProcessor(Class<S> clazz) {
        processorMap.remove(clazz);
        this.removeSignalMerger(clazz);
    }

    @Override
    public <S extends ISignal> void removeSignalMerger(Class<S> clazz) {
        mergerMap.remove(clazz);
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
        this.signalChain = chain;
    }

    @Override
    public void activate() {

    }

    @Override
    public Class<? extends INeuron> getCurrentNeuronClass() {
        return currentNeuronClass;
    }

    @Override
    public Boolean isChanged() {
        return changed;
    }

    @Override
    public void setChanged(Boolean changed) {
        this.changed=changed;

    }

    @Override
    public Boolean isOnDelete() {
        return onDelete;
    }

    @Override
    public void setOnDelete(Boolean onDelete) {
        this.onDelete=onDelete;
    }

    @Override
    public List<CreateNeuronRequest> getCreateRequests() {
        return createNeuronRequests;
    }

    @Override
    public void addCreateRequest(CreateNeuronRequest createNeuronRequest) {
        createNeuronRequests.add(createNeuronRequest);
    }


    private Class<?> getSupperClass(Class<?> clazz) {

        return clazz.getSuperclass();
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }
}
