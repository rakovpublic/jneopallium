/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.ssmaint;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.HealthHypothesisSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.ReconResidualSignal;

import java.util.HashMap;
import java.util.Map;

/**
 * Fuses the reconstruction residual stream into a maintenance hypothesis, all
 * label-free. Per asset it maintains a slow EWMA level and slope of the residual
 * (degradation trend), a Page-Hinkley change-point detector, and an evidence
 * accumulator that only rises when the deviation is persistent, trending,
 * consistent across several sensors, and not merely a domain shift. Severity is
 * expressed in the asset's own baseline-percentile units (calibrated from the
 * healthy warm-up, not from labels), the fault family is attributed
 * heuristically from which sensors dominate the residual, and a lead time is
 * extrapolated from the trend to the baseline limit.
 */
public class MaintenanceHypothesisNeuron extends ModulatableNeuron implements IMaintenanceHypothesisNeuron {

    private static final class State {
        double level;
        double prevLevel;
        double slope;
        boolean seeded;
        // Page-Hinkley
        double phMean;
        double phSum;
        double phMin;
        long n;
        double evidence;
    }

    private final Map<String, State> states = new HashMap<>();

    // baseline (own-history) calibration — global defaults, optional per-asset override
    private double baselineMean = 0.0;
    private double baselineP99 = 1.0;
    private double baselineP999 = 1.5;
    private final Map<String, double[]> assetBaseline = new HashMap<>();  // asset -> {mean,p99,p999}

    private double slowAlpha = 0.02;
    private double evidenceAlpha = 0.08;
    private double phDelta = 0.25;
    private double phLambda = 6.0;
    private double residualConsistencyThreshold = 1.0;

    public MaintenanceHypothesisNeuron() { super(); }
    public MaintenanceHypothesisNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    public void setBaseline(double mean, double p99, double p999) {
        this.baselineMean = mean;
        this.baselineP99 = p99;
        this.baselineP999 = p999;
    }

    public void setAssetBaseline(String assetId, double mean, double p99, double p999) {
        assetBaseline.put(assetId, new double[]{mean, p99, p999});
    }

    // bean accessors so the deployable layer configuration can bind the fitted
    // baseline (own-history) calibration produced by the Python trainer.
    public double getBaselineMean() { return baselineMean; }
    public void setBaselineMean(double baselineMean) { this.baselineMean = baselineMean; }
    public double getBaselineP99() { return baselineP99; }
    public void setBaselineP99(double baselineP99) { this.baselineP99 = baselineP99; }
    public double getBaselineP999() { return baselineP999; }
    public void setBaselineP999(double baselineP999) { this.baselineP999 = baselineP999; }

    public double getSlowAlpha() { return slowAlpha; }
    public void setSlowAlpha(double slowAlpha) { this.slowAlpha = SsMaintMath.clamp01(slowAlpha); }
    public double getEvidenceAlpha() { return evidenceAlpha; }
    public void setEvidenceAlpha(double evidenceAlpha) { this.evidenceAlpha = SsMaintMath.clamp01(evidenceAlpha); }
    public double getPhDelta() { return phDelta; }
    public void setPhDelta(double phDelta) { this.phDelta = phDelta; }
    public double getPhLambda() { return phLambda; }
    public void setPhLambda(double phLambda) { this.phLambda = phLambda; }

    public double evidenceFor(String assetId) {
        State s = states.get(assetId);
        return s == null ? 0.0 : s.evidence;
    }

    @Override
    public HealthHypothesisSignal assess(ReconResidualSignal residual) {
        if (residual == null) return null;
        String asset = residual.getAssetId();
        State st = states.computeIfAbsent(asset, k -> newState());

        double[] base = assetBaseline.getOrDefault(asset,
                new double[]{baselineMean, baselineP99, baselineP999});
        double mean = base[0], p99 = base[1], p999 = base[2];

        double health = residual.getTotal();

        // slow trend + slope
        st.prevLevel = st.level;
        st.level += slowAlpha * (health - st.level);
        double instSlope = st.level - st.prevLevel;
        st.slope += slowAlpha * (instSlope - st.slope);

        // Page-Hinkley change point
        st.n++;
        st.phMean += (health - st.phMean) / st.n;
        st.phSum += health - st.phMean - phDelta;
        st.phMin = Math.min(st.phMin, st.phSum);
        boolean change = (st.phSum - st.phMin) > phLambda;

        // severity in own-history percentile units (>=1.0 == p99 band)
        double denom = Math.max(1e-6, p99 - mean);
        double severity = (st.level - mean) / denom;

        // heuristic family attribution from dominant residuals
        FaultFamily family = attribute(residual);

        // cross-sensor consistency: at least two sensors deviate, or a clear
        // single-sensor disagreement (a sensor fault)
        int deviating = 0;
        for (double r : residual.getResiduals().values()) {
            if (Math.abs(r) > residualConsistencyThreshold) deviating++;
        }
        boolean consistent = deviating >= 2 || family == FaultFamily.SENSOR_FAULT;

        // evidence accumulation
        double target = 0.0;
        if (severity > 0.6 && st.slope > 0.0 && consistent && residual.getDomainShift() < 0.25) {
            target = severity;
        }
        st.evidence += evidenceAlpha * (target - st.evidence);

        // lead time from trend extrapolation to the baseline limit
        long leadTime = 0L;
        if (st.slope > 1e-6 && p999 > st.level) {
            leadTime = (long) Math.max(0.0, (p999 - st.level) / st.slope);
        }

        double uncertainty = SsMaintMath.clamp01(0.2 + residual.getDomainShift()
                + (change ? 0.0 : 0.05));

        return new HealthHypothesisSignal(asset, family.key(), severity, st.evidence,
                leadTime, uncertainty, residual.getDomainShift(), residual.getTimestamp());
    }

    private FaultFamily attribute(ReconResidualSignal residual) {
        Map<String, Double> r = residual.getResiduals();
        double vib = pos(r.get("vibration_rms"));
        double bearing = pos(r.get("bearing_temp"));
        double suction = pos(neg(r.get("suction_pressure")));  // low suction => negative residual
        double power = val(r.get("pump_power"));
        double flow = val(r.get("flow"));
        double procT = val(r.get("process_temp"));
        double valve = val(r.get("valve_position"));

        Map<FaultFamily, Double> score = new HashMap<>();
        score.put(FaultFamily.BEARING_DAMAGE, Math.max(0, vib) + Math.max(0, bearing));
        score.put(FaultFamily.CAVITATION, Math.max(0, suction) + Math.max(0, vib));
        score.put(FaultFamily.SENSOR_FAULT, Math.max(0, Math.abs(procT)
                - 0.5 * (Math.abs(power) + Math.abs(flow))));
        score.put(FaultFamily.ENERGY, Math.max(0, power) - Math.max(0, flow));
        score.put(FaultFamily.OSCILLATION, Math.max(0, Math.abs(valve) - 0.3));

        FaultFamily best = FaultFamily.UNKNOWN_ANOMALY;
        double bestScore = 0.3;   // threshold below which we stay honest (unknown)
        for (Map.Entry<FaultFamily, Double> e : score.entrySet()) {
            if (e.getValue() > bestScore) {
                bestScore = e.getValue();
                best = e.getKey();
            }
        }
        return best;
    }

    private static double val(Double d) { return d == null ? 0.0 : d; }
    private static double pos(double d) { return Math.max(0.0, d); }
    private static double neg(Double d) { return d == null ? 0.0 : -d; }

    private State newState() {
        State s = new State();
        s.level = baselineMean;
        s.prevLevel = baselineMean;
        s.phMean = baselineMean;
        return s;
    }
}
