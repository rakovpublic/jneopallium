/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Hard-wired interlock trip signal. Per spec §5 every trip also emits
 * a TransparencyLogSignal at the dispatch layer.
 * ProcessingFrequency: loop=1, epoch=1.
 */
public class InterlockSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private String interlockId;
    private boolean tripped;
    private List<String> causes;

    public InterlockSignal() { super(); this.loop = 1; this.epoch = 1L; this.timeAlive = 30; this.causes = new ArrayList<>(); }

    public InterlockSignal(String interlockId, boolean tripped, List<String> causes) {
        this();
        this.interlockId = interlockId;
        this.tripped = tripped;
        this.causes = causes == null ? new ArrayList<>() : new ArrayList<>(causes);
    }

    public String getInterlockId() { return interlockId; }
    public void setInterlockId(String i) { this.interlockId = i; }
    public boolean isTripped() { return tripped; }
    public void setTripped(boolean t) { this.tripped = t; }
    public List<String> getCauses() { return Collections.unmodifiableList(causes); }
    public void setCauses(List<String> c) { this.causes = c == null ? new ArrayList<>() : new ArrayList<>(c); }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return InterlockSignal.class; }
    @Override public String getDescription() { return "InterlockSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        InterlockSignal c = new InterlockSignal(interlockId, tripped, causes);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
