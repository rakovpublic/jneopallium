package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.DegradationSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MaintenanceWindowSignal;

public interface IMaintenanceSchedulingNeuron extends IModulatableNeuron {
    /** Propose a maintenance window based on current RUL and a target leadtime. */
    MaintenanceWindowSignal schedule(DegradationSignal rul, long currentTick, long leadTimeTicks);
    int scheduledFor(String assetId);
    void setMinLeadTimeTicks(long t);
    long getMinLeadTimeTicks();
}
