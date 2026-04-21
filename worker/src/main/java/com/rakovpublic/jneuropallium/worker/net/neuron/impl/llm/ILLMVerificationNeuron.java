package com.rakovpublic.jneuropallium.worker.net.neuron.impl.llm;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.Neuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public interface ILLMVerificationNeuron extends INeuron {
    LLMConfidenceSignal verify(LLMResponseSignal responseSignal);
    void addValidator(LLMVerificationNeuron.LLMResponseValidator validator);
    double getApplicabilityThreshold();
    void setApplicabilityThreshold(double applicabilityThreshold);
}
