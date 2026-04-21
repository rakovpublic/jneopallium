package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.ChargeAccumulationSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.StimulationCommandSignal;

public interface IChargeBalanceNeuron extends IModulatableNeuron {
    ChargeAccumulationSignal accumulate(StimulationCommandSignal cmd, double anodicImbalanceFrac);
    boolean exceedsDc(int electrodeId);
    double getNetCharge(int electrodeId);
    void resetElectrode(int electrodeId);
    void setDcToleranceUC(double v);
}
