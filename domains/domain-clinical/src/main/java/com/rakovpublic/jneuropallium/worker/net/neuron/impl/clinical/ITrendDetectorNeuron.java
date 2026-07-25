package com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.VitalSignal;

public interface ITrendDetectorNeuron extends IModulatableNeuron {
    void setWindowSize(int n);
    int getWindowSize();
    void setSlopeUpThreshold(double v);
    double getSlopeUpThreshold();
    TrendDetectorNeuron.Trend observe(VitalSignal v);
    int samplesFor(VitalType t);
    void reset();
}
