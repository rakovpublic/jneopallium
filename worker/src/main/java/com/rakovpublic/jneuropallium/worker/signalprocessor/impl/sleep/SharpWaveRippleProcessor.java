/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.sleep;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.sleep.ISharpWaveRippleNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.sleep.SleepPhase;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.sleep.SharpWaveRippleSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: an incoming {@link SharpWaveRippleSignal} (neuron
 * sequence + power) is offered to an {@link ISharpWaveRippleNeuron},
 * which decides whether the current NREM depth allows re-emission for
 * cortical consolidation. Any emitted burst is forwarded onward.
 */
public class SharpWaveRippleProcessor implements ISignalProcessor<SharpWaveRippleSignal, ISharpWaveRippleNeuron> {

    private static final String DESCRIPTION = "NREM sharp-wave ripple burst for consolidation";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(SharpWaveRippleSignal input, ISharpWaveRippleNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        // The signal's arrival implies NREM3 is active; depth 1.0 is used
        // as a neutral pass-through — the neuron applies its own gating.
        SharpWaveRippleSignal emitted = neuron.maybeEmit(SleepPhase.NREM3, 1.0,
                input.getNeuronSequence(), input.getPower());
        if (emitted != null) out.add((I) emitted);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return SharpWaveRippleProcessor.class; }
    @Override public Class<ISharpWaveRippleNeuron> getNeuronClass() { return ISharpWaveRippleNeuron.class; }
    @Override public Class<SharpWaveRippleSignal> getSignalClass() { return SharpWaveRippleSignal.class; }
}
