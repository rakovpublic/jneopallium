/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.ssmaint;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.OperatorFeedbackSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.ThresholdUpdateSignal;

public interface IFeedbackAdaptationNeuron extends IModulatableNeuron {
    ThresholdUpdateSignal onFeedback(OperatorFeedbackSignal feedback);
    double currentThreshold(String faultFamily);
}
