/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.QuarantineLiftSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.QuarantineRequestSignal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Layer 5 quarantine registry. Enforces the &quot;quarantine is never
 * permanent&quot; invariant from spec §6: every accepted request gets an
 * automatic lift tick; {@link #tick(long)} reaps expired entries and
 * emits the corresponding {@link QuarantineLiftSignal}s. Loop=1 / Epoch=1.
 */
public class QuarantineEntityNeuron extends ModulatableNeuron implements IQuarantineEntityNeuron {

    private static final class Entry { long liftAtTick; String reason; }

    private final Map<String, Entry> active = new HashMap<>();

    public QuarantineEntityNeuron() { super(); }
    public QuarantineEntityNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override
    public boolean apply(QuarantineRequestSignal req, long currentTick) {
        if (req == null || req.getEntityId() == null || req.getDurationTicks() <= 0) return false;
        Entry e = new Entry();
        e.liftAtTick = currentTick + req.getDurationTicks();
        e.reason = req.getReason();
        active.put(req.getEntityId(), e);
        return true;
    }

    @Override
    public boolean reconfirm(String entityId, int additionalTicks, long currentTick) {
        if (entityId == null || additionalTicks <= 0) return false;
        Entry e = active.get(entityId);
        if (e == null) return false;
        long base = Math.max(e.liftAtTick, currentTick);
        e.liftAtTick = base + additionalTicks;
        return true;
    }

    @Override
    public List<QuarantineLiftSignal> tick(long currentTick) {
        List<QuarantineLiftSignal> out = new ArrayList<>();
        Iterator<Map.Entry<String, Entry>> it = active.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Entry> e = it.next();
            if (e.getValue().liftAtTick <= currentTick) {
                out.add(new QuarantineLiftSignal(e.getKey(),
                        "auto-expired:" + (e.getValue().reason == null ? "" : e.getValue().reason)));
                it.remove();
            }
        }
        return out;
    }

    @Override
    public boolean isQuarantined(String entityId, long currentTick) {
        Entry e = active.get(entityId);
        return e != null && e.liftAtTick > currentTick;
    }

    @Override
    public int activeCount(long currentTick) {
        int n = 0;
        for (Entry e : active.values()) if (e.liftAtTick > currentTick) n++;
        return n;
    }
}
