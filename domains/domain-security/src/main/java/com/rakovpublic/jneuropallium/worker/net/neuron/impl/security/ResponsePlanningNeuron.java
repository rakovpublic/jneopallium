/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.QuarantineRequestSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.ThreatHypothesisSignal;

/**
 * Layer 4 response planner. Translates a {@link ThreatHypothesisSignal}
 * posterior into a graduated-response {@link ResponseBand} per spec §6
 * and, when the band calls for quarantine, emits the appropriate
 * {@link QuarantineRequestSignal}. Loop=1 / Epoch=3.
 */
public class ResponsePlanningNeuron extends ModulatableNeuron implements IResponsePlanningNeuron {

    private int defaultDurationTicks = 18_000;
    private int maxDurationTicks = 360_000;

    public ResponsePlanningNeuron() { super(); }
    public ResponsePlanningNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override
    public ResponseBand band(double posterior) {
        if (posterior < 0.30) return ResponseBand.LOG;
        if (posterior < 0.60) return ResponseBand.ALERT;
        if (posterior < 0.85) return ResponseBand.CONNECTION_QUARANTINE;
        return ResponseBand.HOST_QUARANTINE;
    }

    @Override
    public QuarantineRequestSignal plan(ThreatHypothesisSignal t, String entityId, EntityKind kind) {
        if (t == null || entityId == null) return null;
        ResponseBand b = band(t.getPosterior());
        switch (b) {
            case CONNECTION_QUARANTINE:
                return new QuarantineRequestSignal(entityId,
                        kind == null ? EntityKind.CONNECTION : kind,
                        defaultDurationTicks, "hypothesis=" + t.getHypothesisId());
            case HOST_QUARANTINE:
                return new QuarantineRequestSignal(entityId,
                        kind == null ? EntityKind.HOST : kind,
                        Math.min(maxDurationTicks, defaultDurationTicks * 3),
                        "hypothesis=" + t.getHypothesisId());
            default:
                return null;
        }
    }

    @Override public void setDefaultDurationTicks(int t) { this.defaultDurationTicks = Math.max(1, t); }
    @Override public int getDefaultDurationTicks() { return defaultDurationTicks; }
    @Override public void setMaxDurationTicks(int t) { this.maxDurationTicks = Math.max(1, t); }
    @Override public int getMaxDurationTicks() { return maxDurationTicks; }
}
