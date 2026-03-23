package com.rakovpublic.jneuropallium.ai.processors;

import com.rakovpublic.jneuropallium.ai.neurons.loop.RegionMonitorNeuron;
import com.rakovpublic.jneuropallium.ai.signals.fast.ActivityMeasurementSignal;
import com.rakovpublic.jneuropallium.ai.signals.fast.SpikeSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.List;

/** Counts only — never modifies signals. */
public class PassiveCountingProcessor implements ISignalProcessor<SpikeSignal, RegionMonitorNeuron> {

    @Override
    public <I extends ISignal> List<I> process(SpikeSignal input, RegionMonitorNeuron neuron) {
        List<I> results = new ArrayList<>();
        if (!input.isFired()) return results;
        // Record in history ring buffer
        neuron.recordFiring(input.getMagnitude());
        // Compute stats and emit ActivityMeasurementSignal each epoch
        double mean = neuron.computeMean();
        double variance = neuron.computeVariance();
        double trend = neuron.computeTrend();
        ActivityMeasurementSignal activity = new ActivityMeasurementSignal(
            neuron.getMonitoredRegion(), mean, variance, trend, 20);
        activity.setSourceNeuronId(neuron.getId());
        results.add((I) activity);
        return results;
    }

    @Override public String getDescription() { return "PassiveCountingProcessor"; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return PassiveCountingProcessor.class; }
    @Override public Class<RegionMonitorNeuron> getNeuronClass() { return RegionMonitorNeuron.class; }
    @Override public Class<SpikeSignal> getSignalClass() { return SpikeSignal.class; }
}
