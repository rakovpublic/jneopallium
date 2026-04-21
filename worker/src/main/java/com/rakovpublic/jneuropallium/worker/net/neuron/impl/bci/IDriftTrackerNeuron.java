package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.DriftEstimateSignal;
import java.util.HashMap;
import java.util.Map;

public interface IDriftTrackerNeuron extends IModulatableNeuron {
    DriftEstimateSignal observe(int channelId, double shiftScore, double snrLinear);
    boolean needsRecalibration(int channelId);
    double driftFor(int channelId);
    double snrFor(int channelId);
    void setDriftTolerance(double t);
}
