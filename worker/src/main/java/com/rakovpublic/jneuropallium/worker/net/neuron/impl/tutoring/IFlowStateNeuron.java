package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.AffectObservationSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.EngagementSignal;

public interface IFlowStateNeuron extends IModulatableNeuron {
    FlowStateKind classify(double engagement, double valence, double arousal, double recentAccuracy);
    FlowStateKind getCurrentState();
    double getLastEngagement();
    double getLastValence();
    double getLastArousal();
    double getLastAccuracy();

    /**
     * Observation channel: re-classify using the valence / arousal from
     * the affect signal and the last known engagement / accuracy.
     */
    default FlowStateKind observeAffect(AffectObservationSignal s) {
        if (s == null) return getCurrentState();
        return classify(getLastEngagement(), s.getValence(), s.getArousal(), getLastAccuracy());
    }

    /**
     * Observation channel: re-classify using the attention score from
     * the engagement signal and the last known valence / arousal /
     * accuracy.
     */
    default FlowStateKind observeEngagement(EngagementSignal s) {
        if (s == null) return getCurrentState();
        return classify(s.getAttentionScore(), getLastValence(), getLastArousal(), getLastAccuracy());
    }
}
