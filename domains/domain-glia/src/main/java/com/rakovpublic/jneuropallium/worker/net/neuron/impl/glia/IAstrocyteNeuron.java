package com.rakovpublic.jneuropallium.worker.net.neuron.impl.glia;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.glia.CalciumWaveSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.glia.GliotransmitterSignal;

public interface IAstrocyteNeuron extends IModulatableNeuron {
    void accumulate(double activityDelta);
    CalciumWaveSignal maybeEmitWave();
    GliotransmitterSignal release(GliotransmitterType t, double concentration);
    int getRegionId();
    double getIntegratedActivity();
    double getCalciumWaveThreshold();
    void setCalciumWaveThreshold(double v);
    double getPropagationRadius();
    void setPropagationRadius(double v);
}
