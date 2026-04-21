package com.rakovpublic.jneuropallium.worker.net.neuron.impl.llm;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;

public interface ILLMVerificationNeuron extends INeuron {
    LLMConfidenceSignal verify(LLMResponseSignal responseSignal);
    void addValidator(LLMVerificationNeuron.LLMResponseValidator validator);
    double getApplicabilityThreshold();
    void setApplicabilityThreshold(double applicabilityThreshold);
}
