package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;

public interface IAlarmAggregationNeuron extends IModulatableNeuron {
    /** Ingest an alarm; returns null if suppressed / grouped, otherwise the forwarded alarm. */
    AlarmSignal observe(AlarmSignal a);
    int standingAlarmCount();
    double alarmRatePerMin();
    void setSuppressionWindowTicks(long t);
}
