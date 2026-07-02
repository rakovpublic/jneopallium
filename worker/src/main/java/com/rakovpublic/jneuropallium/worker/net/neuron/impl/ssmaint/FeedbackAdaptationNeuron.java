/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.ssmaint;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.OperatorFeedbackSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.ThresholdUpdateSignal;

import java.util.HashMap;
import java.util.Map;

/**
 * Continuous online learning from operator feedback, with no redeploy. Holds a
 * per-fault-family additive offset on top of a static base threshold. A false
 * positive raises that family's threshold (fewer nuisance alerts); a confirmed
 * need relaxes it slightly (recall is already high, so relaxation is small and
 * asymmetric). Every update is bounded (clamped offset), rate-limited (minimum
 * ticks between updates), and frozen during domain shift so novel conditions
 * cannot poison the thresholds. The neuron holds its own state, which the
 * runtime persists to storage — so learning survives restarts without a rebuild.
 */
public class FeedbackAdaptationNeuron extends ModulatableNeuron implements IFeedbackAdaptationNeuron {

    private final Map<String, Double> baseThreshold = new HashMap<>();
    private final Map<String, Double> offset = new HashMap<>();
    private final Map<String, Long> lastUpdateTick = new HashMap<>();

    private double defaultBaseThreshold = 1.0;
    private double stepUp = 0.20;
    private double stepDown = 0.01;
    private double maxOffset = 1.6;
    private double minOffset = -0.1;
    private long rateLimitTicks = 5L;
    private double freezeDomainShift = 0.25;

    public FeedbackAdaptationNeuron() { super(); }
    public FeedbackAdaptationNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    public void setBaseThreshold(String family, double threshold) { baseThreshold.put(family, threshold); }
    public void setDefaultBaseThreshold(double t) { this.defaultBaseThreshold = t; }

    // bean accessors so the deployable layer configuration can bind base
    // thresholds as a map.
    public Map<String, Double> getBaseThresholds() { return baseThreshold; }
    public void setBaseThresholds(Map<String, Double> m) {
        baseThreshold.clear();
        if (m != null) baseThreshold.putAll(m);
    }
    public double getStepUp() { return stepUp; }
    public void setStepUp(double stepUp) { this.stepUp = stepUp; }
    public double getStepDown() { return stepDown; }
    public void setStepDown(double stepDown) { this.stepDown = stepDown; }
    public double getMaxOffset() { return maxOffset; }
    public void setMaxOffset(double maxOffset) { this.maxOffset = maxOffset; }
    public double getMinOffset() { return minOffset; }
    public void setMinOffset(double minOffset) { this.minOffset = minOffset; }
    public long getRateLimitTicks() { return rateLimitTicks; }
    public void setRateLimitTicks(long rateLimitTicks) { this.rateLimitTicks = rateLimitTicks; }
    public double getFreezeDomainShift() { return freezeDomainShift; }
    public void setFreezeDomainShift(double freezeDomainShift) { this.freezeDomainShift = freezeDomainShift; }

    public double offsetFor(String family) { return offset.getOrDefault(family, 0.0); }

    @Override
    public double currentThreshold(String family) {
        return base(family) + offset.getOrDefault(family, 0.0);
    }

    @Override
    public ThresholdUpdateSignal onFeedback(OperatorFeedbackSignal feedback) {
        if (feedback == null || feedback.getFaultFamily() == null) return null;
        String family = feedback.getFaultFamily();

        // anti-poisoning: never adapt during a domain shift
        if (feedback.getDomainShift() > freezeDomainShift) return null;

        // rate-limit per family (null check avoids sentinel-subtraction overflow)
        Long last = lastUpdateTick.get(family);
        if (last != null && feedback.getTimestamp() - last < rateLimitTicks) return null;
        lastUpdateTick.put(family, feedback.getTimestamp());

        double current = offset.getOrDefault(family, 0.0);
        if (feedback.isConfirmed()) {
            current = SsMaintMath.clamp(current - stepDown, minOffset, maxOffset);
        } else {
            current = SsMaintMath.clamp(current + stepUp, minOffset, maxOffset);
        }
        offset.put(family, current);

        return new ThresholdUpdateSignal(family, base(family) + current, current, feedback.getTimestamp());
    }

    private double base(String family) {
        return baseThreshold.getOrDefault(family, defaultBaseThreshold);
    }
}
