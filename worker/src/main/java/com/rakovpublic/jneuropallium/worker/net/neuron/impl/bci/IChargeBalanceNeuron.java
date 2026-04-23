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

    /**
     * Observation channel: an incoming accumulation report (from an
     * upstream balancer) can be cross-checked against the local
     * bookkeeping. Default returns true if the reported electrode's DC
     * drift now exceeds the tolerance.
     */
    default boolean observe(ChargeAccumulationSignal s) {
        return s != null && exceedsDc(s.getElectrodeId());
    }
}
