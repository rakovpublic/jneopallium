package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.CalibrationSignal;

public interface ICalibrationSchedulerNeuron extends IModulatableNeuron {
    CalibrationSignal evaluate(long currentTick, double drift, double decodeError, boolean userAlert);
    void setMinIntervalTicks(long t);
    void setMaxIntervalTicks(long t);
    void setDriftTrigger(double t);
    void setErrorTrigger(double t);
    long getLastCalibrationTick();
    long getSessionCount();
}
