package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.AgencyLossSignal;

public interface IUserStateNeuron extends IModulatableNeuron {
    UserStateNeuron.State classify(double fatigue, double confusion, double distress);
    UserStateNeuron.State getState();
    void setFatigueThreshold(double v);
    void setConfusionThreshold(double v);
    void setDistressThreshold(double v);

    /**
     * Observation channel: an agency-loss report is folded into the
     * distress axis (a proxy — real deployments can refine). Default
     * re-classifies with the mismatch magnitude as distress.
     */
    default UserStateNeuron.State observeAgencyLoss(AgencyLossSignal s) {
        if (s == null) return getState();
        return classify(0.0, 0.0, Math.max(0.0, Math.min(1.0, s.getMismatchMagnitude())));
    }
}
