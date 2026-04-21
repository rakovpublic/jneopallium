package com.rakovpublic.jneuropallium.worker.net.neuron.impl.sleep;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.sleep.SharpWaveRippleSignal;

import java.util.List;

public interface ISharpWaveRippleNeuron extends IModulatableNeuron {
    SharpWaveRippleSignal maybeEmit(SleepPhase phase, double depth, List<Long> neuronSequence, double power);
    double getMinNrem3Depth();
    void setMinNrem3Depth(double v);
}
