package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.LogEventSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.PacketSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.SignatureMatchSignal;

public interface ISignaturePatternNeuron extends IModulatableNeuron {
    void addSignature(String signatureId, String family, byte[] pattern, String referenceIoc);
    SignatureMatchSignal match(PacketSignal p);
    SignatureMatchSignal match(LogEventSignal l);
    int signatureCount();
}
