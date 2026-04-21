package com.rakovpublic.jneuropallium.ai.processors;

import com.rakovpublic.jneuropallium.ai.model.TimestampedSlot;
import com.rakovpublic.jneuropallium.ai.neurons.attention.IWorkingMemoryNeuron;
import com.rakovpublic.jneuropallium.ai.signals.fast.SpikeSignal;
import com.rakovpublic.jneuropallium.ai.signals.fast.WorkingMemoryReadSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.List;

public class WorkingMemoryReadProcessor implements ISignalProcessor<WorkingMemoryReadSignal, IWorkingMemoryNeuron> {

    @Override
    public <I extends ISignal> List<I> process(WorkingMemoryReadSignal input, IWorkingMemoryNeuron neuron) {
        List<I> results = new ArrayList<>();
        TimestampedSlot slot = neuron.getSlots().get(input.getSlotId());
        if (slot != null) {
            SpikeSignal spike = new SpikeSignal(true, 1.0, 1);
            spike.setSourceNeuronId(neuron.getId());
            spike.setName(input.getCallbackNeuronId());
            results.add((I) spike);
        }
        return results;
    }

    @Override public String getDescription() { return "WorkingMemoryReadProcessor"; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return WorkingMemoryReadProcessor.class; }
    @Override public Class<IWorkingMemoryNeuron> getNeuronClass() { return IWorkingMemoryNeuron.class; }
    @Override public Class<WorkingMemoryReadSignal> getSignalClass() { return WorkingMemoryReadSignal.class; }
}
