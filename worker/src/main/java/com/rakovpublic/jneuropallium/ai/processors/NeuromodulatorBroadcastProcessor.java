package com.rakovpublic.jneuropallium.ai.processors;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.signals.slow.NeuromodulatorSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.List;

public class NeuromodulatorBroadcastProcessor implements ISignalProcessor<NeuromodulatorSignal, IModulatableNeuron> {

    @Override
    public <I extends ISignal> List<I> process(NeuromodulatorSignal input, IModulatableNeuron neuron) {
        switch (input.getType()) {
            case DOPAMINE:       neuron.setDopamineLevel(input.getConcentration()); break;
            case SEROTONIN:      neuron.setErrorDampeningFactor(input.getConcentration()); break;
            case NOREPINEPHRINE: neuron.setNorepinephrineLevel(input.getConcentration()); break;
            case ACETYLCHOLINE:  neuron.setAchLevel(input.getConcentration()); break;
            case GABA:           neuron.setInhibitionLevel(input.getConcentration()); break;
        }
        return new ArrayList<>();
    }

    @Override public String getDescription() { return "NeuromodulatorBroadcastProcessor"; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return NeuromodulatorBroadcastProcessor.class; }
    @Override public Class<IModulatableNeuron> getNeuronClass() { return IModulatableNeuron.class; }
    @Override public Class<NeuromodulatorSignal> getSignalClass() { return NeuromodulatorSignal.class; }
}
