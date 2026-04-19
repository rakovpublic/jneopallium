/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.DifficultyLevel;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Fires when an item (question, exercise, prompt) is presented to the
 * learner. ProcessingFrequency: loop=1, epoch=1.
 */
public class ItemPresentationSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private String itemId;
    private String conceptId;
    private DifficultyLevel difficulty;
    private long presentedAt;

    public ItemPresentationSignal() {
        super();
        this.loop = 1;
        this.epoch = 1L;
        this.timeAlive = 100;
        this.difficulty = DifficultyLevel.MEDIUM;
    }

    public ItemPresentationSignal(String itemId, String conceptId, DifficultyLevel difficulty, long presentedAt) {
        this();
        this.itemId = itemId;
        this.conceptId = conceptId;
        this.difficulty = difficulty == null ? DifficultyLevel.MEDIUM : difficulty;
        this.presentedAt = presentedAt;
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public String getConceptId() { return conceptId; }
    public void setConceptId(String conceptId) { this.conceptId = conceptId; }
    public DifficultyLevel getDifficulty() { return difficulty; }
    public void setDifficulty(DifficultyLevel d) { this.difficulty = d == null ? DifficultyLevel.MEDIUM : d; }
    public long getPresentedAt() { return presentedAt; }
    public void setPresentedAt(long presentedAt) { this.presentedAt = presentedAt; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return ItemPresentationSignal.class; }
    @Override public String getDescription() { return "ItemPresentationSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        ItemPresentationSignal c = new ItemPresentationSignal(itemId, conceptId, difficulty, presentedAt);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
