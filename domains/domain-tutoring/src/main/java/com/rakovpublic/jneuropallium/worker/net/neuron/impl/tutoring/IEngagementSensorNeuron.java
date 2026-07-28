package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.EngagementSignal;

public interface IEngagementSensorNeuron extends IModulatableNeuron {
    EngagementSignal fuse(double clickRate, double dwellTime, double camera, double mic);
    double getFusedAttention();
    double getClickRateScore();
    double getDwellTimeScore();
    double getCameraScore();
    double getMicScore();
}
