package com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;

public interface IGuidelineMemoryNeuron extends IModulatableNeuron {
    void store(GuidelineMemoryNeuron.Guideline g);
    GuidelineMemoryNeuron.Guideline lookup(String icd10);
    boolean isFirstLine(String icd10, String rxNormOrProcedureCode);
    boolean isContraindicatedByGuideline(String icd10, String code);
    String citeFor(String icd10);
    int size();
    void clear();
}
