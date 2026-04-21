package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

public interface IKalmanDecoderNeuron extends IModulatableNeuron {
    double step(double y);
    double getPos();
    double getVel();
    void setProcessNoise(double p);
    void setMeasurementNoise(double m);
    void setObservationCoefficient(double c);
    void reset();
}
