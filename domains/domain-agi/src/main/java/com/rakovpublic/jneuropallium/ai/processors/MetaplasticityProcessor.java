package com.rakovpublic.jneuropallium.ai.processors;

import com.rakovpublic.jneuropallium.ai.neurons.learning.IMetaplasticityNeuron;
import com.rakovpublic.jneuropallium.ai.signals.fast.ActivityMeasurementSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.List;

public class MetaplasticityProcessor implements ISignalProcessor<ActivityMeasurementSignal, IMetaplasticityNeuron> {

    @Override
    public <I extends ISignal> List<I> process(ActivityMeasurementSignal input, IMetaplasticityNeuron neuron) {
        neuron.getActivityHistory().put(input.getRegionId(), input.getMeanFiringRate());
        if (input.getMeanFiringRate() > neuron.getOveractiveThreshold()) {
            neuron.reduceSlasticityRate(input.getRegionId());
        } else if (input.getMeanFiringRate() < neuron.getUnderactiveThreshold()) {
            neuron.increaseSlasticityRate(input.getRegionId());
        }
        return new ArrayList<>();
    }

    @Override public String getDescription() { return "MetaplasticityProcessor"; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return MetaplasticityProcessor.class; }
    @Override public Class<IMetaplasticityNeuron> getNeuronClass() { return IMetaplasticityNeuron.class; }
    @Override public Class<ActivityMeasurementSignal> getSignalClass() { return ActivityMeasurementSignal.class; }
}
