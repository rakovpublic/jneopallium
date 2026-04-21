package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.ContentRecommendationSignal;

import java.util.List;

public interface IZPDPlanningNeuron extends IModulatableNeuron {
    ContentRecommendationSignal plan(List<ZPDPlanningNeuron.Candidate> candidates);
    void setTargetSuccessRate(double r);
    double getTargetSuccessRate();
}
