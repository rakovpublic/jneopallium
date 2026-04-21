package com.rakovpublic.jneuropallium.ai.neurons.attention;

import com.rakovpublic.jneuropallium.ai.model.TimestampedSlot;
import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;

import java.util.Map;

public interface IWorkingMemoryNeuron extends IModulatableNeuron {
    Map<String, TimestampedSlot> getSlots();
    void setSlots(Map<String, TimestampedSlot> slots);
    int getMaxSlots();
    void setMaxSlots(int maxSlots);
}
