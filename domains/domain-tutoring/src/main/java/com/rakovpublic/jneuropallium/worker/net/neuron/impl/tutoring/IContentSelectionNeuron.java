package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.ContentRecommendationSignal;

import java.util.List;

public interface IContentSelectionNeuron extends IModulatableNeuron {
    String select(List<ContentSelectionNeuron.ScoredItem> items);
    String argmax(List<ContentSelectionNeuron.ScoredItem> items);
    void setBetaNovelty(double b);
    double getBetaNovelty();
    void setTemperature(double t);
    double getTemperature();

    /**
     * Observation channel: record an external ZPD recommendation so
     * the selection neuron can align future choices. Default no-op.
     */
    default void observe(ContentRecommendationSignal s) { /* no-op by default */ }
}
