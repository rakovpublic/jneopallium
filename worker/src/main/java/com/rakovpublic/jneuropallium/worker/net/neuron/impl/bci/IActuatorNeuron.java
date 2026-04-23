package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.SensoryFeedbackSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.StimulationCommandSignal;

public interface IActuatorNeuron extends IModulatableNeuron {
    boolean dispatch(StimulationCommandSignal cmd, long currentTick);
    long getDispatchedCount();
    long getLastDispatchTick();
    void reset();

    /**
     * Observation channel: a sensory-feedback cue is recorded for
     * transparency / audit. Default just increments the dispatch count
     * so the cue is visible in deployment metrics.
     */
    default void observeFeedback(SensoryFeedbackSignal s) { /* no-op by default */ }
}
