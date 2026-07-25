package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.ActuatorCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.SetpointSignal;

public interface IPIDNeuron extends IModulatableNeuron {
    void setGains(double kp, double ki, double kd);
    double getKp();
    double getKi();
    double getKd();
    /** Scale current gains in place (temporary tuning — {@code LoopCircuitBreaker} SCALE_WEIGHTS intervention). */
    void scaleGains(double factor);
    void setSetpoint(SetpointSignal s);
    double getSetpoint();
    void setOutputLimits(double min, double max);
    /** Process one measurement and emit the corresponding actuator command. {@code dtSeconds} is the loop period. */
    ActuatorCommandSignal step(MeasurementSignal m, double dtSeconds);
    void reset();
    double getLastError();
    double getLastOutput();
}
