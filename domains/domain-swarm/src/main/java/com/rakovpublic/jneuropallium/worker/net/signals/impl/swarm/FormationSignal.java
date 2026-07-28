/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm.FormationTemplate;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/** Formation slot assignment for the receiving agent. ProcessingFrequency: loop=1, epoch=2. */
public class FormationSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(2L, 1);

    private FormationTemplate template;
    private int slotIndex;
    private double[] relativeOffset;

    public FormationSignal() {
        super();
        this.loop = 1;
        this.epoch = 2L;
        this.timeAlive = 200;
        this.template = FormationTemplate.FREE;
    }

    public FormationSignal(FormationTemplate template, int slotIndex, double[] relativeOffset) {
        this();
        this.template = template == null ? FormationTemplate.FREE : template;
        this.slotIndex = slotIndex;
        this.relativeOffset = relativeOffset == null ? null : relativeOffset.clone();
    }

    public FormationTemplate getTemplate() { return template; }
    public void setTemplate(FormationTemplate t) { this.template = t == null ? FormationTemplate.FREE : t; }
    public int getSlotIndex() { return slotIndex; }
    public void setSlotIndex(int s) { this.slotIndex = s; }
    public double[] getRelativeOffset() { return relativeOffset == null ? null : relativeOffset.clone(); }
    public void setRelativeOffset(double[] r) { this.relativeOffset = r == null ? null : r.clone(); }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return FormationSignal.class; }
    @Override public String getDescription() { return "FormationSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        FormationSignal c = new FormationSignal(template, slotIndex, relativeOffset);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
