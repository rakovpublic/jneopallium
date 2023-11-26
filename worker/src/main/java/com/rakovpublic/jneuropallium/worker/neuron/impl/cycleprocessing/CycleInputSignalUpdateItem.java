/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.neuron.impl.cycleprocessing;

public class CycleInputSignalUpdateItem {
    private ProcessingFrequency frequency;
    private String name;

    public CycleInputSignalUpdateItem(ProcessingFrequency frequency, String name) {
        this.frequency = frequency;
        this.name = name;
    }

    public ProcessingFrequency getFrequency() {
        return frequency;
    }

    public void setFrequency(ProcessingFrequency frequency) {
        this.frequency = frequency;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
