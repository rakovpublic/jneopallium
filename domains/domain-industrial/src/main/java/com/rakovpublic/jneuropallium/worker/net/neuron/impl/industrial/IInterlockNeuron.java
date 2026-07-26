package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.ActuatorCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.InterlockSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;

import java.util.List;

public interface IInterlockNeuron extends IModulatableNeuron {
    /** Record immutable interlock rule (active after construction); throws if invoked after {@link #seal()}. */
    void addInterlock(String interlockId, String measurementTag, double threshold,
                      boolean tripHigh, String safeActuatorTag, double safeValue);
    /** Freeze the interlock set. Subsequent {@code addInterlock} calls throw. */
    void seal();
    boolean isSealed();
    /** Evaluate a measurement against all interlocks; returns trip signal list when triggered. */
    List<InterlockSignal> evaluate(MeasurementSignal m);
    /** Fail-safe commands emitted by the most recent trips, intended to bypass planning direct-to-actuator. */
    List<ActuatorCommandSignal> failSafeCommands();
}
