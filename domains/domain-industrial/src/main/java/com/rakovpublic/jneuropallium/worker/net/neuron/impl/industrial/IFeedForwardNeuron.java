package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.ActuatorCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;

public interface IFeedForwardNeuron extends IModulatableNeuron {
    void setGain(double k);
    double getGain();
    /** Apply a disturbance reading to produce a corrective bias on the downstream actuator. */
    ActuatorCommandSignal compensate(MeasurementSignal disturbance, String targetTag, double currentValue);
}
