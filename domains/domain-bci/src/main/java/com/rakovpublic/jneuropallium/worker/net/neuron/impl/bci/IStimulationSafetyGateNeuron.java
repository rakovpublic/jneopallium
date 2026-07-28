package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.StimulationCommandSignal;

public interface IStimulationSafetyGateNeuron extends IModulatableNeuron {
    void setElectrodeArea(int electrodeId, double cm2);
    void setMaxChargeDensityUCcm2(double v);
    void setMaxChargePerPhaseUC(double v);
    void setMaxFrequencyHz(double v);
    void setMaxPulseWidthUS(double v);
    void triggerSeizureLockout(long untilTick);
    void triggerThermalLockout(long untilTick);
    void clearLockouts(long currentTick);
    boolean isLocked();
    String veto(StimulationCommandSignal cmd, long currentTick);
}
