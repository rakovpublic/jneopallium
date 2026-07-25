package com.rakovpublic.jneuropallium.ai.neurons.attention;

import com.rakovpublic.jneuropallium.ai.model.TimestampedSlot;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Working memory neuron used by WorkingMemoryWriteProcessor and WorkingMemoryReadProcessor.
 * Maintains a bounded map of named, timestamped memory slots.
 */
public class WorkingMemoryNeuron extends ModulatableNeuron implements IWorkingMemoryNeuron {

    private Map<String, TimestampedSlot> slots;
    private int maxSlots;

    public WorkingMemoryNeuron() {
        super();
        this.slots = new LinkedHashMap<>();
        this.maxSlots = 7;
    }

    public WorkingMemoryNeuron(Long neuronId,
                               com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain chain,
                               Long run,
                               int maxSlots) {
        super(neuronId, chain, run);
        this.slots = new LinkedHashMap<>();
        this.maxSlots = maxSlots;
    }

    public Map<String, TimestampedSlot> getSlots() { return slots; }
    public void setSlots(Map<String, TimestampedSlot> slots) { this.slots = slots; }

    public int getMaxSlots() { return maxSlots; }
    public void setMaxSlots(int maxSlots) { this.maxSlots = maxSlots; }
}
