/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.DegradationSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MaintenanceWindowSignal;

import java.util.HashMap;
import java.util.Map;

/** Layer 4 maintenance scheduler. Loop=2 / Epoch=10. */
public class MaintenanceSchedulingNeuron extends ModulatableNeuron implements IMaintenanceSchedulingNeuron {

    private final Map<String, Integer> scheduled = new HashMap<>();
    private long minLeadTimeTicks = 10_000L;
    private long ticksPerHour = 360_000L; // 100Hz baseline

    public MaintenanceSchedulingNeuron() { super(); }
    public MaintenanceSchedulingNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    public void setTicksPerHour(long t) { this.ticksPerHour = Math.max(1L, t); }

    @Override
    public MaintenanceWindowSignal schedule(DegradationSignal rul, long currentTick, long leadTimeTicks) {
        if (rul == null || rul.getAssetId() == null) return null;
        long rulTicks = (long) (rul.getRemainingUsefulLifeHours() * ticksPerHour);
        long lead = Math.max(leadTimeTicks, minLeadTimeTicks);
        long scheduledTick = Math.max(currentTick, currentTick + rulTicks - lead);
        int duration = (int) Math.min(Integer.MAX_VALUE, Math.max(1L, ticksPerHour));
        scheduled.merge(rul.getAssetId(), 1, Integer::sum);
        return new MaintenanceWindowSignal(rul.getAssetId(), scheduledTick, duration);
    }

    @Override public int scheduledFor(String assetId) { return scheduled.getOrDefault(assetId, 0); }
    @Override public void setMinLeadTimeTicks(long t) { this.minLeadTimeTicks = Math.max(0L, t); }
    @Override public long getMinLeadTimeTicks() { return minLeadTimeTicks; }
}
