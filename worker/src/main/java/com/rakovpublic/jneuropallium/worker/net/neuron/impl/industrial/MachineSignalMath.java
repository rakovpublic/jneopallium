/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import java.util.LinkedHashMap;
import java.util.Map;

final class MachineSignalMath {
    private MachineSignalMath() {}

    static final class FeatureStats {
        final double rms;
        final double peakAbs;
        final double meanAbs;
        final double crestFactor;
        final double zeroCrossingRate;
        final double spectralCentroidHz;
        final double envelopeEnergy;

        FeatureStats(double rms, double peakAbs, double meanAbs, double crestFactor,
                     double zeroCrossingRate, double spectralCentroidHz, double envelopeEnergy) {
            this.rms = rms;
            this.peakAbs = peakAbs;
            this.meanAbs = meanAbs;
            this.crestFactor = crestFactor;
            this.zeroCrossingRate = zeroCrossingRate;
            this.spectralCentroidHz = spectralCentroidHz;
            this.envelopeEnergy = envelopeEnergy;
        }

        Map<String, Double> toMap() {
            Map<String, Double> out = new LinkedHashMap<>();
            out.put("rms", rms);
            out.put("peakAbs", peakAbs);
            out.put("meanAbs", meanAbs);
            out.put("crestFactor", crestFactor);
            out.put("zeroCrossingRate", zeroCrossingRate);
            out.put("spectralCentroidHz", spectralCentroidHz);
            out.put("envelopeEnergy", envelopeEnergy);
            return out;
        }
    }

    static FeatureStats analyse(double[] samples, double sampleRateHz) {
        if (samples == null || samples.length == 0) {
            return new FeatureStats(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }
        double sumSq = 0.0;
        double sumAbs = 0.0;
        double peakAbs = 0.0;
        double envelope = 0.0;
        int crossings = 0;
        double previous = samples[0];
        for (int i = 0; i < samples.length; i++) {
            double value = finite(samples[i]);
            double abs = Math.abs(value);
            sumSq += value * value;
            sumAbs += abs;
            peakAbs = Math.max(peakAbs, abs);
            if (i > 0) {
                if ((previous < 0.0 && value >= 0.0) || (previous >= 0.0 && value < 0.0)) crossings++;
                envelope += Math.abs(value - previous);
            }
            previous = value;
        }
        double rms = Math.sqrt(sumSq / samples.length);
        double meanAbs = sumAbs / samples.length;
        double crest = peakAbs / Math.max(1e-9, rms);
        double zcr = samples.length <= 1 ? 0.0 : crossings / (double) (samples.length - 1);
        double envelopeEnergy = samples.length <= 1 ? 0.0 : envelope / (samples.length - 1);
        double centroid = spectralCentroid(samples, sampleRateHz);
        return new FeatureStats(rms, peakAbs, meanAbs, crest, zcr, centroid, envelopeEnergy);
    }

    static double spectralCentroid(double[] samples, double sampleRateHz) {
        if (samples == null || samples.length < 4 || sampleRateHz <= 0.0) return 0.0;
        int n = samples.length;
        int bins = Math.min(48, n / 2);
        double weighted = 0.0;
        double total = 0.0;
        for (int k = 1; k <= bins; k++) {
            double real = 0.0;
            double imag = 0.0;
            for (int i = 0; i < n; i++) {
                double angle = -2.0 * Math.PI * k * i / n;
                double value = finite(samples[i]);
                real += value * Math.cos(angle);
                imag += value * Math.sin(angle);
            }
            double magnitude = Math.sqrt(real * real + imag * imag);
            double frequency = k * sampleRateHz / n;
            weighted += frequency * magnitude;
            total += magnitude;
        }
        return total <= 1e-12 ? 0.0 : weighted / total;
    }

    static double clamp01(double value) {
        if (!Double.isFinite(value)) return 0.0;
        return Math.max(0.0, Math.min(1.0, value));
    }

    static double finite(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }
}
