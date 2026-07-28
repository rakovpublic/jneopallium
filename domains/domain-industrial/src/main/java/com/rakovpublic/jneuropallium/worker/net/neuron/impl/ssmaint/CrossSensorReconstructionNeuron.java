/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.ssmaint;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.AssetTelemetrySignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.ReconResidualSignal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The self-supervised core. For each sensor it holds a ridge-regression model
 * (fitted offline on a trusted-healthy window) that predicts that sensor from
 * the others, after standardising by operating regime. At runtime the residual
 * between prediction and observation is the anomaly signal: a large total means
 * the joint sensor pattern no longer looks healthy; a large single-sensor
 * residual means that sensor disagrees with its peers (a candidate sensor
 * fault). No fault labels are used — the target of each regression is another
 * sensor, so training needs only ordinary operating telemetry.
 *
 * <p>Parameters ({@code sensorOrder}, {@code regimeMeans}, {@code regimeStds},
 * {@code crossWeights}) are produced by the Python initial-training step and
 * carried in the layer configuration; they can also be set directly for tests.
 */
public class CrossSensorReconstructionNeuron extends ModulatableNeuron implements ICrossSensorReconstructionNeuron {

    private List<String> sensorOrder;
    private double[][] regimeMeans;      // [regime][sensor]
    private double[][] regimeStds;       // [regime][sensor]
    private double[][] crossWeights;     // [sensor][ (S-1) predictors + bias ]
    private double domainShiftZ = 6.0;

    public CrossSensorReconstructionNeuron() { super(); }
    public CrossSensorReconstructionNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    public List<String> getSensorOrder() { return sensorOrder; }
    public void setSensorOrder(List<String> sensorOrder) { this.sensorOrder = sensorOrder; }
    public double[][] getRegimeMeans() { return regimeMeans; }
    public void setRegimeMeans(double[][] regimeMeans) { this.regimeMeans = regimeMeans; }
    public double[][] getRegimeStds() { return regimeStds; }
    public void setRegimeStds(double[][] regimeStds) { this.regimeStds = regimeStds; }
    public double[][] getCrossWeights() { return crossWeights; }
    public void setCrossWeights(double[][] crossWeights) { this.crossWeights = crossWeights; }
    public double getDomainShiftZ() { return domainShiftZ; }
    public void setDomainShiftZ(double domainShiftZ) { this.domainShiftZ = domainShiftZ; }

    @Override
    public ReconResidualSignal reconstruct(AssetTelemetrySignal telemetry) {
        if (telemetry == null || sensorOrder == null || crossWeights == null
                || regimeMeans == null || regimeStds == null) {
            return null;
        }
        int s = sensorOrder.size();
        int regime = clampRegime(telemetry.getRegime());
        Map<String, Double> sensors = telemetry.getSensors();

        // standardise by regime
        double[] z = new double[s];
        for (int i = 0; i < s; i++) {
            Double raw = sensors.get(sensorOrder.get(i));
            double mean = regimeMeans[regime][i];
            double std = Math.max(1e-6, regimeStds[regime][i]);
            z[i] = raw == null ? 0.0 : (raw - mean) / std;
        }

        // per-sensor reconstruction residuals from the other sensors
        Map<String, Double> residuals = new LinkedHashMap<>();
        double sumSq = 0.0;
        int domainShiftCount = 0;
        for (int i = 0; i < s; i++) {
            double[] predictors = new double[s - 1];
            int k = 0;
            for (int j = 0; j < s; j++) {
                if (j != i) predictors[k++] = z[j];
            }
            double predicted = SsMaintMath.linear(crossWeights[i], predictors);
            double res = z[i] - predicted;
            residuals.put(sensorOrder.get(i), res);
            sumSq += res * res;
            if (Math.abs(z[i]) > domainShiftZ) domainShiftCount++;
        }
        double total = sumSq / s;
        double domainShift = (double) domainShiftCount / s;

        return new ReconResidualSignal(telemetry.getAssetId(), regime, total, domainShift,
                residuals, telemetry.getTimestamp());
    }

    private int clampRegime(int regime) {
        if (regimeMeans == null || regimeMeans.length == 0) return 0;
        if (regime < 0) return 0;
        if (regime >= regimeMeans.length) return regimeMeans.length - 1;
        return regime;
    }
}
