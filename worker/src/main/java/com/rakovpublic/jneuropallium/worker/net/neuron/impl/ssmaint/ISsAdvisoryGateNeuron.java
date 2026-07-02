/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.ssmaint;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.HealthHypothesisSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.MaintenanceAdvisorySignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.ThresholdUpdateSignal;

public interface ISsAdvisoryGateNeuron extends IModulatableNeuron {
    MaintenanceAdvisorySignal gate(HealthHypothesisSignal hypothesis);
    void onThresholdUpdate(ThresholdUpdateSignal update);
}
