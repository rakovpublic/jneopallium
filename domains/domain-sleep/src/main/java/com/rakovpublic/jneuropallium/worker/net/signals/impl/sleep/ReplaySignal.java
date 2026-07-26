/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.sleep;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.sleep.ReplayDirection;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Compressed replay of a recorded episode sequence. A replay carries the
 * sequence identifier, playback direction, and compression ratio relative
 * to original wall-clock duration.
 * <p>Biological analogue: hippocampal replay events (Wilson &amp;
 * McNaughton 1994; Foster &amp; Wilson 2006).
 * <p>ProcessingFrequency: loop=2, epoch=3.
 */
public class ReplaySignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(3L, 2);

    private String sequenceId;
    private ReplayDirection direction;
    private double compressionRatio;
    private List<Long> neuronSequence = new ArrayList<>();

    public ReplaySignal() {
        super();
        this.loop = 2;
        this.epoch = 3L;
        this.timeAlive = 100;
        this.direction = ReplayDirection.REVERSE;
        this.compressionRatio = 1.0;
    }

    public ReplaySignal(String sequenceId, ReplayDirection direction, double compressionRatio, List<Long> neuronSequence) {
        this();
        this.sequenceId = sequenceId;
        this.direction = direction == null ? ReplayDirection.REVERSE : direction;
        this.compressionRatio = Math.max(1.0, compressionRatio);
        this.neuronSequence = (neuronSequence == null) ? new ArrayList<>() : new ArrayList<>(neuronSequence);
    }

    public String getSequenceId() { return sequenceId; }
    public void setSequenceId(String sequenceId) { this.sequenceId = sequenceId; }

    public ReplayDirection getDirection() { return direction; }
    public void setDirection(ReplayDirection direction) { this.direction = direction == null ? ReplayDirection.REVERSE : direction; }

    public double getCompressionRatio() { return compressionRatio; }
    public void setCompressionRatio(double compressionRatio) { this.compressionRatio = Math.max(1.0, compressionRatio); }

    public List<Long> getNeuronSequence() { return Collections.unmodifiableList(neuronSequence); }
    public void setNeuronSequence(List<Long> neuronSequence) {
        this.neuronSequence = (neuronSequence == null) ? new ArrayList<>() : new ArrayList<>(neuronSequence);
    }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return ReplaySignal.class; }
    @Override public String getDescription() { return "ReplaySignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        ReplaySignal c = new ReplaySignal(sequenceId, direction, compressionRatio, neuronSequence);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
