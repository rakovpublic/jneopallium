package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;

public interface IOscillationMonitorNeuron extends IModulatableNeuron {
    void observe(MeasurementSignal m);
    /** Normalised severity in [0,1] — 0 means no oscillation, 1 is maximum. */
    double severity(String tag);
    /** Spec §6 intervention band at the current severity. */
    OscillationIntervention intervention(String tag);
    void setAcfWindowTicks(int w);
    int getAcfWindowTicks();
}
