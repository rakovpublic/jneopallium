package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.DegradationSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;

public interface IDegradationModelNeuron extends IModulatableNeuron {
    void seedAsset(String assetId, double initialRulHours);
    /** Ingest a wear-related measurement for {@code assetId} and emit an updated RUL estimate. */
    DegradationSignal observe(String assetId, MeasurementSignal wear);
    double rulFor(String assetId);
    int trackedAssets();
}
