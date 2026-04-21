package com.rakovpublic.jneuropallium.worker.net.neuron.impl.affect;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.affect.AffectStateSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.affect.AppraisalSignal;

public interface IAffectIntegrationNeuron extends IModulatableNeuron, IAffectiveNeuron {
    AffectStateSignal integrate(double goalDelta, double novelty, double controllability, double homeostaticError, double painMagnitude);
    AffectState currentState();
    void modulateThreshold(double arousalFactor);
    void onAppraisal(AppraisalSignal s);
    String getContextId();
    void setContextId(String contextId);
    double getValence();
    double getArousal();
    double getFiringThreshold();
    double getBaselineThreshold();
}
