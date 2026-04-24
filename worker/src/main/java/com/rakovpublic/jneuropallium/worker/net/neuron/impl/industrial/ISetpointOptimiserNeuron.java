package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.EfficiencySignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.SetpointSignal;

public interface ISetpointOptimiserNeuron extends IModulatableNeuron {
    void setConstraint(String tag, double min, double max);
    void setStep(double step);
    /** Propose a setpoint nudge for {@code tag} based on the efficiency delta. */
    SetpointSignal optimise(EfficiencySignal eff, String tag, double currentSetpoint);
}
