package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.ReviewScheduleSignal;

public interface IForgettingCurveNeuron extends IModulatableNeuron {
    double retention(String conceptId, long nowTick);
    ReviewScheduleSignal recordAttempt(String conceptId, long nowTick, int quality);
    void setTargetRetention(double r);
    double getTargetRetention();
    void setBaseIntervalTicks(long t);
    int trackedConcepts();
    long intervalFor(String conceptId);
    int repetitionsFor(String conceptId);

    /**
     * Observation channel: align target retention with an
     * externally-scheduled review so both agents agree. Default no-op.
     */
    default void observe(ReviewScheduleSignal s) { /* no-op by default */ }
}
