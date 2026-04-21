package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
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
}
