/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.embodiment;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.signals.slow.HarmFeedbackSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.EfferenceCopySignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.ProprioceptiveSignal;

import java.util.HashMap;
import java.util.Map;

/**
 * Compares a stored efference copy against actual proprioceptive feedback
 * and emits a {@link HarmFeedbackSignal} when the mismatch crosses a
 * failure-emit threshold.
 * Layer 1, loop=1 / epoch=1.
 * <p>Biological analogue: cerebellar Purkinje-cell comparator.
 */
public class ReafferenceComparatorNeuron extends ModulatableNeuron implements IEmbodied, IReafferenceComparatorNeuron {

    private final Map<Integer, EfferenceCopySignal> pendingByEffector = new HashMap<>();
    private BodySchema schema = new BodySchema();
    private double mismatchThreshold = 0.15;
    private double failureEmitThreshold = 0.4;
    private double lastMismatch;

    public ReafferenceComparatorNeuron() { super(); }

    public ReafferenceComparatorNeuron(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
    }

    public void registerEfferenceCopy(EfferenceCopySignal s) {
        if (s == null) return;
        pendingByEffector.put(s.getEffectorId(), s);
    }

    @Override public BodySchema currentSchema() { return schema; }

    @Override
    public void onProprioceptive(ProprioceptiveSignal p) {
        if (p == null) return;
        EfferenceCopySignal expected = pendingByEffector.remove(p.getEffectorId());
        if (expected == null) return;
        double[] predicted = expected.getPredictedOutcome();
        double[] actual = p.getJointStates();
        int n = Math.min(predicted.length, actual.length);
        double sumSq = 0;
        for (int i = 0; i < n; i++) {
            double d = predicted[i] - actual[i];
            sumSq += d * d;
        }
        int maxLen = Math.max(predicted.length, actual.length);
        if (maxLen == 0) {
            lastMismatch = 0;
            return;
        }
        lastMismatch = Math.sqrt(sumSq / Math.max(1, n));
    }

    /**
     * Evaluate the last mismatch and, when over threshold, emit a
     * hardware-failure harm feedback.
     *
     * @param actionPlanId id of the action plan associated with the motor command
     * @return a {@link HarmFeedbackSignal} when the mismatch exceeds the
     *         failure-emit threshold; {@code null} otherwise.
     */
    public HarmFeedbackSignal maybeEmitFailure(String actionPlanId) {
        if (lastMismatch < failureEmitThreshold) return null;
        HarmFeedbackSignal fb = new HarmFeedbackSignal(actionPlanId, true,
                new double[]{lastMismatch, 0, 0, 0, 0}, "mechanical");
        fb.setSourceNeuronId(this.getId());
        return fb;
    }

    public double getLastMismatch() { return lastMismatch; }
    public double getMismatchThreshold() { return mismatchThreshold; }
    public void setMismatchThreshold(double mismatchThreshold) { this.mismatchThreshold = mismatchThreshold; }
    public double getFailureEmitThreshold() { return failureEmitThreshold; }
    public void setFailureEmitThreshold(double failureEmitThreshold) { this.failureEmitThreshold = failureEmitThreshold; }
    public int pendingCount() { return pendingByEffector.size(); }
}
