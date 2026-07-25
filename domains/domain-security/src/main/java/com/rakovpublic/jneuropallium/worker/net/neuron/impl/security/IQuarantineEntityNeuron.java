package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.QuarantineLiftSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.QuarantineRequestSignal;

import java.util.List;

public interface IQuarantineEntityNeuron extends IModulatableNeuron {
    /**
     * Apply a quarantine request. Returns true if accepted (and scheduled
     * for automatic lift at {@code currentTick + durationTicks}), false if
     * the request duration was zero/negative (rejected as permanent).
     */
    boolean apply(QuarantineRequestSignal req, long currentTick);

    /** Reconfirm an active quarantine, extending it in place. */
    boolean reconfirm(String entityId, int additionalTicks, long currentTick);

    /** Emit lift signals for entities whose quarantine expired at or before {@code currentTick}. */
    List<QuarantineLiftSignal> tick(long currentTick);

    boolean isQuarantined(String entityId, long currentTick);
    int activeCount(long currentTick);
}
