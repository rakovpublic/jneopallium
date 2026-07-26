/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.ActuatorCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.OperatorOverrideSignal;

import java.util.HashMap;
import java.util.Map;

/**
 * Layer 5 actuator writer. Per spec §5 "operator override always wins"
 * for regulatory control — a MANUAL override freezes the tag at the
 * operator's manual value and a BYPASS override disables the loop
 * entirely. Loop=1 / Epoch=1.
 */
public class ActuatorNeuron extends ModulatableNeuron implements IActuatorNeuron {

    private final Map<String, OperatorOverrideSignal> overrides = new HashMap<>();
    private final Map<String, Double> lastDispatched = new HashMap<>();
    private long dispatched;
    private long blocked;

    public ActuatorNeuron() { super(); }
    public ActuatorNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override public void onOverride(OperatorOverrideSignal o) {
        if (o == null || o.getTag() == null) return;
        overrides.put(o.getTag(), o);
    }

    @Override public void clearOverride(String tag) { if (tag != null) overrides.remove(tag); }
    @Override public boolean isOverridden(String tag) { return overrides.containsKey(tag); }
    @Override public OverrideKind overrideKind(String tag) {
        OperatorOverrideSignal o = overrides.get(tag);
        return o == null ? null : o.getKind();
    }

    @Override
    public boolean dispatch(ActuatorCommandSignal cmd) {
        if (cmd == null || cmd.getTag() == null) return false;
        OperatorOverrideSignal o = overrides.get(cmd.getTag());
        if (o != null) {
            if (o.getKind() == OverrideKind.BYPASS) { blocked++; return false; }
            // MANUAL: freeze at the operator's value
            if (!cmd.isExecute()) return false;
            lastDispatched.put(cmd.getTag(), o.getManualValue());
            dispatched++;
            return true;
        }
        if (!cmd.isExecute()) { blocked++; return false; }
        lastDispatched.put(cmd.getTag(), cmd.getTargetValue());
        dispatched++;
        return true;
    }

    @Override public long getDispatched() { return dispatched; }
    @Override public long getBlocked() { return blocked; }
    @Override public Double lastDispatchedValue(String tag) { return lastDispatched.get(tag); }
}
