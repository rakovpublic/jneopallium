package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.BatchStateSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.InterlockSignal;

public interface IModeControllerNeuron extends IModulatableNeuron {
    PlantMode getMode();
    void requestMode(PlantMode requested);
    void onBatchState(BatchStateSignal s);
    void onInterlock(InterlockSignal s);
    boolean allowsNormalControl();
}
