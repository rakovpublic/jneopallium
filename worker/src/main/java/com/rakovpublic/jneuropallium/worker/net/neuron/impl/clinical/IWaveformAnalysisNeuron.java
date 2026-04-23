package com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.AdverseEventAlertSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.WaveformSignal;

public interface IWaveformAnalysisNeuron extends IModulatableNeuron {
    AdverseEventAlertSignal analyse(WaveformSignal w);
    double getLastRms();
    double getLastZcr();
    double getLastPeakToPeak();
    long getAnalysedCount();
}
