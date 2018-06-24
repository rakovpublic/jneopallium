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

    public Neuron(Long neuronId, ISignalChain processingChain) {
        this.neuronId = neuronId;
        isProcessed = false;
        signals = new ArrayList<>();
        result = new ArrayList<>();
        processorHashMap = new HashMap<>();
        mergerHashMap = new HashMap<>();
        this.processingChain = processingChain;
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
                List<ISignal> ll = new ArrayList<>();
                ll.add(s);
                signalsMap.put(cl, ll);
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
                result.add(signalProcessor.process(inS, this));
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
}
