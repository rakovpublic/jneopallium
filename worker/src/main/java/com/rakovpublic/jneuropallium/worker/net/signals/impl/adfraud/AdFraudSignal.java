/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.adfraud;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.adfraud.AdFraudDecision;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.adfraud.AdFraudEvent;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class AdFraudSignal extends AbstractSignal<AdFraudEvent> implements ISignal<AdFraudEvent>, IInputSignal<AdFraudEvent> {
    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);
    private Class<? extends ISignal<AdFraudEvent>> currentSignalClass;
    private AdFraudEvent event;
    private AdFraudDecision decision;

    public AdFraudSignal() {
        this(AdFraudSignal.class, new ProcessingFrequency(1L, 1), null);
    }

    protected AdFraudSignal(Class<? extends ISignal<AdFraudEvent>> currentSignalClass,
                            ProcessingFrequency frequency,
                            AdFraudEvent event) {
        super();
        this.currentSignalClass = currentSignalClass;
        this.event = event;
        this.value = event;
        this.loop = frequency.getLoop();
        this.epoch = frequency.getEpoch();
        this.timeAlive = 100;
    }

    public AdFraudSignal(AdFraudEvent event) {
        this(AdFraudSignal.class, PROCESSING_FREQUENCY, event);
    }

    public AdFraudEvent getEvent() { return event; }
    public void setEvent(AdFraudEvent event) { this.event = event; this.value = event; }
    public AdFraudDecision getDecision() { return decision; }
    public void setDecision(AdFraudDecision decision) { this.decision = decision; }

    @Override public AdFraudEvent getValue() { return event; }
    @Override public Class<AdFraudEvent> getParamClass() { return AdFraudEvent.class; }
    @Override public Class<? extends ISignal<AdFraudEvent>> getCurrentSignalClass() { return currentSignalClass; }
    @Override public String getDescription() { return currentSignalClass.getSimpleName(); }
    @Override public boolean canUseProcessorForParent() { return true; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<AdFraudEvent>> K copySignal() {
        AdFraudSignal copy = new AdFraudSignal(currentSignalClass, new ProcessingFrequency(epoch, loop), event);
        copy.setDecision(decision);
        copy.sourceLayer = this.sourceLayer;
        copy.sourceNeuron = this.sourceNeuron;
        copy.name = this.name;
        copy.inputName = this.inputName;
        return (K) copy;
    }
}

