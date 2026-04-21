package com.rakovpublic.jneuropallium.worker.net.neuron.impl.sleep;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.sleep.SleepStateSignal;

public interface ISleepControllerNeuron extends IModulatableNeuron {
    SleepStateSignal advance();
    void setPhase(SleepPhase phase);
    SleepPhase currentPhase();
    long getTick();
    int getCycleTicks();
    void setCycleTicks(int v);
    double getNremFraction();
    void setNremFraction(double v);
    double getRemFraction();
    void setRemFraction(double v);
}
