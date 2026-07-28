package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.InterventionSignal;

public interface IFatigueNeuron extends IModulatableNeuron {
    void startSession(long tick);
    InterventionSignal tick(long tick, boolean lastAnswerCorrect, double errorRate);
    long getSessionStartTick();
    long getCurrentTick();
    int getConsecutiveErrors();
    double getCurrentErrorRate();
    void setBaselineErrorRate(double r);
    void setMaxSessionTicks(int t);
    void setMaxConsecutiveErrors(int n);
}
