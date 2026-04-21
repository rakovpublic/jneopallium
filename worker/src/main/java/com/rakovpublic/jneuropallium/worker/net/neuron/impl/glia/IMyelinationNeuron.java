package com.rakovpublic.jneuropallium.worker.net.neuron.impl.glia;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.glia.MyelinationSignal;
import java.util.HashMap;
import java.util.Map;

public interface IMyelinationNeuron extends IModulatableNeuron {
    void recordUsage(long targetNeuronId);
    MyelinationSignal evaluate(long sourceNeuronId, long targetNeuronId);
    void applyTo(DelayedAxon axon, MyelinationSignal sig);
    void demyelinate(DelayedAxon axon, long targetNeuronId);
    void resetWindow();
    int currentDelayFor(long targetNeuronId);
    int getBaselineDelayTicks();
    void setBaselineDelayTicks(int v);
    int getMinDelayTicks();
    void setMinDelayTicks(int v);
    int getActivityWindow();
    void setActivityWindow(int v);
    int getDelayDecrementPerWindow();
    void setDelayDecrementPerWindow(int v);
    int getUsageThreshold();
    void setUsageThreshold(int v);
    double getConsolidationBoost();
    void setConsolidationBoost(double v);
}
