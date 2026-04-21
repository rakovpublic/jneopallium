package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.LFPSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.SeizureRiskSignal;

public interface ISeizureWatchdogNeuron extends IModulatableNeuron {
    SeizureRiskSignal assess(LFPSignal lfp, int regionId);
    boolean shouldTriggerLockout(double risk);
    long getLockoutDurationTicks();
    void setRiskThreshold(double t);
    void setLockoutDurationTicks(long t);
    double getLastRisk();
    SeizureMarker getLastMarker();
}
