package com.rakovpublic.jneuropallium.ai.processors;

import com.rakovpublic.jneuropallium.ai.model.TimestampedSlot;
import com.rakovpublic.jneuropallium.ai.neurons.attention.IWorkingMemoryNeuron;
import com.rakovpublic.jneuropallium.ai.signals.fast.WorkingMemoryWriteSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WorkingMemoryWriteProcessor implements ISignalProcessor<WorkingMemoryWriteSignal, IWorkingMemoryNeuron> {

    @Override
    public <I extends ISignal> List<I> process(WorkingMemoryWriteSignal input, IWorkingMemoryNeuron neuron) {
        Map<String, TimestampedSlot> slots = neuron.getSlots();
        if (slots.size() >= neuron.getMaxSlots() && !slots.containsKey(input.getSlotId())) {
            // Evict lowest salience slot
            Optional<Map.Entry<String, TimestampedSlot>> minEntry = slots.entrySet().stream()
                .min((a, b) -> Double.compare(a.getValue().getSalience(), b.getValue().getSalience()));
            minEntry.ifPresent(e -> slots.remove(e.getKey()));
        }
        slots.put(input.getSlotId(), new TimestampedSlot(input.getContent(), input.getTtl()));
        return new ArrayList<>();
    }

    @Override public String getDescription() { return "WorkingMemoryWriteProcessor"; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return WorkingMemoryWriteProcessor.class; }
    @Override public Class<IWorkingMemoryNeuron> getNeuronClass() { return IWorkingMemoryNeuron.class; }
    @Override public Class<WorkingMemoryWriteSignal> getSignalClass() { return WorkingMemoryWriteSignal.class; }
}
