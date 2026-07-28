package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.ItemPresentationSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.MasteryUpdateSignal;

public interface IConceptMasteryNeuron extends IModulatableNeuron {
    MasteryUpdateSignal update(boolean correct);
    String getConceptId();
    void setConceptId(String c);
    double getPKnow();
    void setPKnow(double v);
    double getPLearn();
    void setPLearn(double v);
    double getPSlip();
    void setPSlip(double v);
    double getPGuess();
    void setPGuess(double v);
    void setEmitDelta(double d);

    /**
     * Observation channel: a new item presentation refocuses the
     * mastery neuron on the presented concept. Default updates the
     * concept id if the signal carries a non-null one.
     */
    default void observe(ItemPresentationSignal s) {
        if (s != null && s.getConceptId() != null) setConceptId(s.getConceptId());
    }
}
