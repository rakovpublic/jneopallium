package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.EfficiencySignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;

public interface IEnergyAccountingNeuron extends IModulatableNeuron {
    /** Record energy usage for a unit. */
    void recordEnergy(String unitId, double kwh);
    /** Record the production rate (baseline for attribution). */
    void recordProduction(String unitId, double units);
    EfficiencySignal efficiencyFor(String unitId);
    /** Convenience: update from a raw measurement. */
    void observe(MeasurementSignal m);
}
