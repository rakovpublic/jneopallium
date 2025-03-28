package com.rakovpublic.jneuropallium.worker.net.neuron.impl;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rakovpublic.jneuropallium.worker.exceptions.CannotFindSignalMergerException;
import com.rakovpublic.jneuropallium.worker.exceptions.CannotFindSignalProcessorException;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayer;
import com.rakovpublic.jneuropallium.worker.net.neuron.*;
import com.rakovpublic.jneuropallium.worker.net.signals.IChangingSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignalHistoryStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;


public class Neuron implements INeuron {
    @JsonIgnore
    public List<ISignal> signals;
    public Boolean isProcessed;
    public Dendrites dendrites;
    public Axon axon;
    public List<Class<? extends ISignal>> resultClasses;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public HashMap<Class<? extends ISignal>, IActivationFunction> activationFunctions;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public HashMap<Class<? extends ISignal>, ISignalProcessor> processorMap;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public HashMap<Class<? extends ISignal>, ISignalMerger> mergerMap;
    @JsonIgnore
    public ILayer layer;
    public Long neuronId;
    @JsonIgnore
    public List<ISignal> result;
    public ISignalChain signalChain;
    public List<IRule> rules;
    public Class<? extends INeuron> currentNeuronClass;
    public Boolean changed;
    public Boolean onDelete;
    public Long run;
    @JsonIgnore
    public ISignalHistoryStorage signalHistory;

    @Override
    public HashMap<String, Long> getCyclingNeuronInputMapping() {
        return cyclingNeuronInputMapping;
    }

    @Override
    public void setCyclingNeuronInputMapping(HashMap<String, Long> cyclingNeuronInputMapping) {
        this.cyclingNeuronInputMapping = cyclingNeuronInputMapping;
    }

    @JsonIgnore
    public HashMap<String, Long> cyclingNeuronInputMapping;

    public Integer currentLoopAmount;

    @Override
    public Integer getCurrentLoop() {
        return currentLoopAmount;
    }

    @Override
    public void setCurrentLoop(Integer currentLoopAmount) {
        this.currentLoopAmount = currentLoopAmount;
    }

    @Override
    public boolean canProcess(ISignal signal) {
        Set<Class<? extends ISignal>> canProcess = processorMap.keySet();
        if (canProcess.contains(signal.getClass())) {
            return true;
        } else if (signal.canUseProcessorForParent()) {
            Class<?> currentClass = signal.getClass().getSuperclass();
            while (currentClass != ISignal.class && currentClass != Object.class) {
                if (canProcess.contains(currentClass)) {
                    return true;
                }
                currentClass = currentClass.getSuperclass();
            }
        }
        return false;
    }

    @Override
    public boolean canProcess(Class<? extends ISignal> signal) {
        Set<Class<? extends ISignal>> canProcess = processorMap.keySet();
        return canProcess.contains(signal);
    }

    @Override
    public void setLayer(ILayer layer) {
        this.layer = layer;
    }

    @Override
    public ILayer getLayer() {
        return layer;
    }

    @Override
    public List<Class<? extends ISignal>> getResultClasses() {
        return resultClasses;
    }


    public Neuron() {
        rules = new ArrayList<>();
        isProcessed = false;
        signals = new ArrayList<>();
        result = new ArrayList<>();
        processorMap = new HashMap<>();
        mergerMap = new HashMap<>();
        currentNeuronClass = Neuron.class;
        activationFunctions = new HashMap<>();
        resultClasses = new LinkedList<>();
        run = 0l;
        onDelete = false;
        changed = false;
        currentLoopAmount = 0;
        cyclingNeuronInputMapping = new HashMap<>();
    }

    public Neuron(Long neuronId, ISignalChain processingChain, Long run) {
        cyclingNeuronInputMapping = new HashMap<>();
        rules = new ArrayList<>();
        this.neuronId = neuronId;
        isProcessed = false;
        signals = new ArrayList<>();
        result = new ArrayList<>();
        processorMap = new HashMap<>();
        mergerMap = new HashMap<>();
        activationFunctions = new HashMap<>();
        this.signalChain = processingChain;
        this.run = run;
        resultClasses = new LinkedList<>();
        onDelete = false;
        changed = false;
        currentLoopAmount = 0;

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
    public Long getRun() {
        return run;
    }

    @Override
    public void setRun(Long run) {
        this.run = run;
    }

    @Override
    public ISignalHistoryStorage getSignalHistory() {
        return signalHistory;
    }


    @Override
    public void setSignalHistory(ISignalHistoryStorage signalHistory) {
        this.signalHistory = signalHistory;
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

        this.signals.addAll(signals.stream().map(s -> {
            s.setCurrentInnerLoop(currentLoopAmount);
            return s;
        }).collect(Collectors.toList()));
    }

    //TODO:refactor this method
    @Override
    public void processSignals() {
        if (cyclingNeuronInputMapping == null) {
            cyclingNeuronInputMapping = new HashMap<>();
        }
        try {
            HashMap<Class<? extends ISignal>, List<ISignal>> signalsMap = new HashMap<>();
            if (dendrites == null) {
                dendrites = new Dendrites();
            }
            List<ISignal> signalsForProcessing = dendrites.processSignalsWithDendrites(signals);
            for (ISignal s : signalsForProcessing) {

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
                    for (ISignal signal : signalsMap.get(cls)) {
                        if (signal instanceof IChangingSignal) {
                            IChangingSignal cs = (IChangingSignal) signal;
                            if (cs.canProcess(this.getClass())) {
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
                        while (clst.getSuperclass() != ISignal.class && clst.getSuperclass() != Object.class && s.canUseProcessorForParent() && done) {
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
                            clst = clst.getSuperclass();
                        }
                    }

                }
            }
        } catch (Exception e) {
            Logger logger = LogManager.getLogger(Neuron.class);
            logger.error("Error during neuron processing", e);
        }
    }

    @Override
    public void setAxon(Axon axon) {
        this.axon = axon;
    }

    @Override
    public <S extends ISignal, N extends INeuron> void addSignalProcessor(Class<S> clazz, ISignalProcessor<S, N> processor) {
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
        isProcessed = false;
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
    public Axon getAxon() {
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
        List<ISignal> newResult = new LinkedList<>();
        for (ISignal s : result) {
            if (activationFunctions.containsKey(s.getCurrentSignalClass())) {
                Optional<ISignal> sig = activationFunctions.get(s.getCurrentSignalClass()).activate(s);
                if (sig.isPresent()) {
                    newResult.add(s);
                }
            } else {
                newResult.add(s);
            }
        }
        result = newResult;
        isProcessed = true;
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
        this.changed = changed;

    }

    public Dendrites getDendrites() {
        return dendrites;
    }

    public void setDendrites(Dendrites dendrites) {
        this.dendrites = dendrites;
    }

    @Override
    public Boolean isOnDelete() {
        return onDelete;
    }

    @Override
    public void setOnDelete(Boolean onDelete) {
        this.onDelete = onDelete;
    }

    @Override
    public <I extends ISignal> void addActivationFunction(Class<I> clazz, IActivationFunction<I> activationFunction) {
        activationFunctions.put(clazz, activationFunction);
        resultClasses.add(clazz);
    }

    @Override
    public void setActivationFunctions(HashMap<Class<? extends ISignal>, IActivationFunction> functions) {
        this.activationFunctions = functions;
    }
}
