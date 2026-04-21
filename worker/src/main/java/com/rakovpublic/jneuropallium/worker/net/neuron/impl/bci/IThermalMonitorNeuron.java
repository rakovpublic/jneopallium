package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.ThermalSignal;

public interface IThermalMonitorNeuron extends IModulatableNeuron {
    boolean observe(ThermalSignal s);
    boolean isCoolDown();
    boolean isShutdown();
    double getLastDelta();
    void setCoolDownDeltaC(double d);
    void setShutdownDeltaC(double d);
    void reset();
}
