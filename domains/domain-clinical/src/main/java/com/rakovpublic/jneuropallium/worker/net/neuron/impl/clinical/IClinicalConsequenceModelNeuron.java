package com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;

public interface IClinicalConsequenceModelNeuron extends IModulatableNeuron {
    ClinicalConsequenceModelNeuron.Forecast simulate(
            double doseMg, double weightKg,
            ClinicalConsequenceModelNeuron.PkPdParams p, double vulnerability);
}
