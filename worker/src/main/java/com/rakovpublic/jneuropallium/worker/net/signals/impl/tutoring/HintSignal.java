/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.HintLevel;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Graduated hint emitted for the current item. Levels progress from
 * least-informative (meta-cognitive) to most-informative (worked example).
 * ProcessingFrequency: loop=1, epoch=2.
 */
public class HintSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(2L, 1);

    private String itemId;
    private HintLevel level;
    private String hintText;

    public HintSignal() {
        super();
        this.loop = 1;
        this.epoch = 2L;
        this.timeAlive = 100;
        this.level = HintLevel.META_COGNITIVE;
    }

    public HintSignal(String itemId, HintLevel level, String hintText) {
        this();
        this.itemId = itemId;
        this.level = level == null ? HintLevel.META_COGNITIVE : level;
        this.hintText = hintText;
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public HintLevel getLevel() { return level; }
    public void setLevel(HintLevel l) { this.level = l == null ? HintLevel.META_COGNITIVE : l; }
    public String getHintText() { return hintText; }
    public void setHintText(String hintText) { this.hintText = hintText; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return HintSignal.class; }
    @Override public String getDescription() { return "HintSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        HintSignal c = new HintSignal(itemId, level, hintText);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
