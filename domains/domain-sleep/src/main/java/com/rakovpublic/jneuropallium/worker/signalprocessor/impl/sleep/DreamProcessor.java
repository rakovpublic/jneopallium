/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.sleep;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.sleep.IREMDreamingNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.sleep.SleepPhase;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.sleep.DreamSignal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: an incoming {@link DreamSignal} (new candidate
 * episode bindings) is fed as a single-episode list into the
 * {@link IREMDreamingNeuron#recombine} recombination pipeline. Only
 * dreams below the max-novelty threshold are forwarded as candidate
 * plans — higher-novelty ones stay off the planning path, per the
 * sleep-safety invariant.
 */
public class DreamProcessor implements ISignalProcessor<DreamSignal, IREMDreamingNeuron> {

    private static final String DESCRIPTION = "REM dream recombination into planning candidates";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(DreamSignal input, IREMDreamingNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        List<List<Long>> episodes = new ArrayList<>();
        episodes.add(input.getEpisodeBindings());
        List<DreamSignal> recombined = neuron.recombine(SleepPhase.REM,
                Collections.unmodifiableList(episodes));
        if (recombined != null) {
            for (DreamSignal d : recombined) {
                if (d != null && neuron.isPlanningCandidate(d)) out.add((I) d);
            }
        }
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return DreamProcessor.class; }
    @Override public Class<IREMDreamingNeuron> getNeuronClass() { return IREMDreamingNeuron.class; }
    @Override public Class<DreamSignal> getSignalClass() { return DreamSignal.class; }
}
