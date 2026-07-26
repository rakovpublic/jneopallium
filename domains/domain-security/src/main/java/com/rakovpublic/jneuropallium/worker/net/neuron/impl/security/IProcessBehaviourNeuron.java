package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.SignatureMatchSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.SyscallSignal;

public interface IProcessBehaviourNeuron extends IModulatableNeuron {
    void addForbiddenSequence(String id, int[] syscallNums);
    SignatureMatchSignal observe(SyscallSignal s);
    int ruleCount();
    void resetFor(int pid);
}
