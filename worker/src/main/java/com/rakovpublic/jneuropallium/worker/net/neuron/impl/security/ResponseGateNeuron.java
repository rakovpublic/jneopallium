/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.QuarantineRequestSignal;

import java.util.HashSet;
import java.util.Set;

/**
 * Layer 5 response gate. Immutable (constructor-time) hard-allow list
 * and critical-asset list prevent &quot;friendly fire&quot; per spec §5.
 * Modes: enforcing, monitor-only, alert-only. Loop=1 / Epoch=1.
 */
public class ResponseGateNeuron extends ModulatableNeuron implements IResponseGateNeuron {

    private final Set<String> hardAllow = new HashSet<>();
    private final Set<String> criticalAssets = new HashSet<>();
    private String mode = "enforcing";
    private double safePosterior = 0.85;
    private double uncertainPosterior = 0.60;

    public ResponseGateNeuron() { super(); }
    public ResponseGateNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    public void setPosteriorBands(double uncertain, double safe) {
        this.uncertainPosterior = Math.max(0.0, Math.min(1.0, uncertain));
        this.safePosterior = Math.max(this.uncertainPosterior, Math.min(1.0, safe));
    }

    @Override
    public QuarantineRequestSignal gate(QuarantineRequestSignal req, double posterior) {
        if (req == null) return null;
        if ("monitor-only".equalsIgnoreCase(mode)) return null;
        if (isHardAllowed(req.getEntityId())) return null;
        if (isCritical(req.getEntityId()) && posterior < safePosterior) return null;
        if ("alert-only".equalsIgnoreCase(mode)) return null;
        if (posterior < uncertainPosterior) return null;
        if (posterior < safePosterior) return null;
        return req;
    }

    @Override public void registerHardAllow(String entityPattern) {
        if (entityPattern != null) hardAllow.add(entityPattern);
    }
    @Override public void registerCriticalAsset(String entityId) {
        if (entityId != null) criticalAssets.add(entityId);
    }

    @Override
    public boolean isHardAllowed(String entityId) {
        if (entityId == null) return false;
        for (String pattern : hardAllow) {
            if (pattern.endsWith("*")) {
                String p = pattern.substring(0, pattern.length() - 1);
                if (entityId.startsWith(p)) return true;
            } else if (pattern.equals(entityId)) return true;
        }
        return false;
    }

    @Override public boolean isCritical(String entityId) { return entityId != null && criticalAssets.contains(entityId); }
    @Override public String getMode() { return mode; }
    @Override public void setMode(String mode) {
        if (mode == null) return;
        if (!"enforcing".equalsIgnoreCase(mode)
                && !"monitor-only".equalsIgnoreCase(mode)
                && !"alert-only".equalsIgnoreCase(mode)) {
            throw new IllegalArgumentException("mode must be enforcing, monitor-only, or alert-only");
        }
        this.mode = mode.toLowerCase();
    }
}
