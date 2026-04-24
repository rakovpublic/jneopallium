/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.SelfToleranceSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.SignatureMatchSignal;

import java.util.HashSet;
import java.util.Set;

/**
 * Layer 1 inhibitory interneuron: suppresses signature matches against
 * entities on the soft allow-list. Biological analogue: peripheral
 * negative selection of self-reactive clones.
 * Loop=1 / Epoch=1.
 */
public class InnateInterneuron extends ModulatableNeuron implements IInnateInterneuron {

    private final Set<String> allowList = new HashSet<>();

    public InnateInterneuron() { super(); }
    public InnateInterneuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override
    public void onTolerance(SelfToleranceSignal s) {
        if (s == null || s.getEntityPattern() == null) return;
        if (s.isAllow()) allowList.add(s.getEntityPattern());
        else allowList.remove(s.getEntityPattern());
    }

    @Override
    public SignatureMatchSignal filter(SignatureMatchSignal match, String entityId) {
        if (match == null) return null;
        if (entityId != null && isAllowed(entityId)) return null;
        return match;
    }

    @Override
    public boolean isAllowed(String entityId) {
        if (entityId == null) return false;
        for (String pattern : allowList) {
            if (pattern == null) continue;
            if (pattern.endsWith("*")) {
                String p = pattern.substring(0, pattern.length() - 1);
                if (entityId.startsWith(p)) return true;
            } else if (pattern.equals(entityId)) return true;
        }
        return false;
    }

    @Override public int allowRuleCount() { return allowList.size(); }
}
