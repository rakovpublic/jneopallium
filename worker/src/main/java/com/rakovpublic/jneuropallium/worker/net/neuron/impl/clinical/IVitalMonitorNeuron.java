package com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.AdverseEventAlertSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.VitalSignal;

public interface IVitalMonitorNeuron extends IModulatableNeuron {
    void setGuardrail(VitalType t, double min, double max);
    AdverseEventAlertSignal observe(VitalSignal v);
    Double lastValue(VitalType t);
    double[] getGuardrail(VitalType t);
}
