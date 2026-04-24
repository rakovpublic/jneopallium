package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;

public interface IMeasurementValidatorNeuron extends IModulatableNeuron {
    void setRange(String tag, double min, double max);
    void setMaxRateOfChange(String tag, double unitsPerSecond);
    /** Validate in-place; may downgrade quality to UNCERTAIN but never drops the reading. */
    MeasurementSignal validate(MeasurementSignal m);
    int suspiciousCount();
}
