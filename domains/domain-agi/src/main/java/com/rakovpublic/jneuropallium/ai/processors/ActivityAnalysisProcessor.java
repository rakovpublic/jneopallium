package com.rakovpublic.jneuropallium.ai.processors;

import com.rakovpublic.jneuropallium.ai.enums.LoopType;
import com.rakovpublic.jneuropallium.ai.neurons.loop.ILoopDetectorNeuron;
import com.rakovpublic.jneuropallium.ai.signals.fast.ActivityMeasurementSignal;
import com.rakovpublic.jneuropallium.ai.signals.fast.LoopAlertSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.List;

public class ActivityAnalysisProcessor implements ISignalProcessor<ActivityMeasurementSignal, ILoopDetectorNeuron> {

    private static final double POSITIVE_THRESHOLD = 0.15;
    private static final double NEGATIVE_THRESHOLD = 0.10;
    private static final double MAX_THEORETICAL_RATE = 100.0;

    @Override
    public <I extends ISignal> List<I> process(ActivityMeasurementSignal input, ILoopDetectorNeuron neuron) {
        List<I> results = new ArrayList<>();
        String region = input.getRegionId();
        List<ActivityMeasurementSignal> history = neuron.getRegionHistory()
            .computeIfAbsent(region, k -> new ArrayList<>());
        history.add(input);
        if (history.size() > neuron.getHistoryWindowSize()) history.remove(0);

        if (history.size() >= 3) {
            // Check POSITIVE_RUNAWAY
            boolean allPositive = true;
            for (ActivityMeasurementSignal h : history) {
                if (h.getTrend() <= POSITIVE_THRESHOLD) { allPositive = false; break; }
            }
            double latestMean = input.getMeanFiringRate();
            if (allPositive && latestMean > 0.85 * MAX_THEORETICAL_RATE) {
                LoopAlertSignal alert = new LoopAlertSignal(LoopType.POSITIVE_RUNAWAY, region, region, 0.9, 0);
                alert.setSourceNeuronId(neuron.getId());
                results.add((I) alert);
            }
            // Check NEGATIVE_RUNAWAY
            double baseline = neuron.getBaselineRate(region);
            boolean allNegative = true;
            for (ActivityMeasurementSignal h : history) {
                if (h.getTrend() >= -NEGATIVE_THRESHOLD) { allNegative = false; break; }
            }
            if (allNegative && latestMean < 0.05 * baseline) {
                LoopAlertSignal alert = new LoopAlertSignal(LoopType.NEGATIVE_RUNAWAY, region, region, 0.6, 0);
                alert.setSourceNeuronId(neuron.getId());
                results.add((I) alert);
            }
        }
        return results;
    }

    @Override public String getDescription() { return "ActivityAnalysisProcessor"; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return ActivityAnalysisProcessor.class; }
    @Override public Class<ILoopDetectorNeuron> getNeuronClass() { return ILoopDetectorNeuron.class; }
    @Override public Class<ActivityMeasurementSignal> getSignalClass() { return ActivityMeasurementSignal.class; }
}
