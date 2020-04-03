package net.neuron.impl;

import exceptions.CannotFindSignalMergerException;
import exceptions.CannotFindSignalProcessorException;
import net.neuron.*;
import net.signals.ISignal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public  class Neuron implements INeuron {
    private List<ISignal> signals;
    private Boolean isProcessed;
    private IAxon axon;
    private HashMap<Class<? extends ISignal>, ISignalProcessor> processorHashMap;
    private HashMap<Class<? extends ISignal>, ISignalMerger> mergerHashMap;
    private Long neuronId;
    protected List<ISignal> result;
    private ISignalChain processingChain;
    private List<IRule> rules;
    private Class<?extends INeuron> currentNeuronClass;

    public Neuron() {
        rules = new ArrayList<>();
        isProcessed = false;
        signals = new ArrayList<>();
        result = new ArrayList<>();
        processorHashMap = new HashMap<>();
        mergerHashMap = new HashMap<>();
        currentNeuronClass=Neuron.class;
    }

    public Neuron(Long neuronId, ISignalChain processingChain) {
        rules = new ArrayList<>();
        this.neuronId = neuronId;
        isProcessed = false;
        signals = new ArrayList<>();
        result = new ArrayList<>();
        processorHashMap = new HashMap<>();
        mergerHashMap = new HashMap<>();
        this.processingChain = processingChain;
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
            Class<? extends ISignal> cl = s.getCurrentSignalClass();
            if (signalsMap.containsKey(cl)) {
                signalsMap.get(cl).add(s);
            } else {
                List<ISignal> ll = new ArrayList<>();
                ll.add(s);
                signalsMap.put(cl, ll);
            }
        }
        for (Class<? extends ISignal> cl : processingChain.getProcessingChain()) {
            for (Class<? extends ISignal> cls : signalsMap.keySet()) {
                if (cl.equals(cls)) {
                    ISignalMerger signalMerger = mergerHashMap.get(cl);
                    ISignalProcessor signalProcessor = processorHashMap.get(cl);
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
                } else if (!processingChain.getProcessingChain().contains(cls)) {
                    ISignal s = signalsMap.get(cls).get(0);
                    Class<?> clst = cls;
                    boolean done = true;
                    while (getSupperClass(clst) != ISignal.class && getSupperClass(clst) != Object.class && s.canUseProcessorForParent() && done) {
                        if (cl.equals(clst)) {
                            ISignalMerger signalMerger = mergerHashMap.get(cl);
                            ISignalProcessor signalProcessor = processorHashMap.get(cl);
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

    @Override
    public void activate() {

    }

    @Override
    public Class<? extends INeuron> getCurrentNeuronClass() {
        return currentNeuronClass;
    }


    private Class<?> getSupperClass(Class<?> clazz) {

        return clazz.getSuperclass();
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }
}
