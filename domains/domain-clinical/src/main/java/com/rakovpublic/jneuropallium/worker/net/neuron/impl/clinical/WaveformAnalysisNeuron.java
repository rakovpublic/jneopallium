/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.AdverseEventAlertSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.WaveformSignal;

/**
 * Layer 0 waveform analyser. Runs small real-time detectors over
 * {@link WaveformSignal} streams (ECG arrhythmia, PPG perfusion, EEG
 * burst-suppression). This implementation is an algorithmic stub that
 * extracts summary features (mean, RMS, peak-to-peak, zero-crossing rate)
 * and fires an alert only on clearly pathological excursions — a real
 * deployment would plug in a validated model here. Loop=1 / Epoch=1.
 */
public class WaveformAnalysisNeuron extends ModulatableNeuron implements IWaveformAnalysisNeuron {

    private double lastRms;
    private double lastZcr;
    private double lastPeakToPeak;
    private long analysed;

    public WaveformAnalysisNeuron() { super(); }

    public WaveformAnalysisNeuron(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
    }

    public AdverseEventAlertSignal analyse(WaveformSignal w) {
        if (w == null) return null;
        double[] s = w.getSamples();
        if (s == null || s.length == 0) return null;
        analysed++;
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        double sumSq = 0.0;
        int zc = 0;
        double prev = s[0];
        for (int i = 0; i < s.length; i++) {
            double v = s[i];
            if (v < min) min = v;
            if (v > max) max = v;
            sumSq += v * v;
            if (i > 0 && ((prev >= 0 && v < 0) || (prev < 0 && v >= 0))) zc++;
            prev = v;
        }
        lastRms = Math.sqrt(sumSq / s.length);
        lastPeakToPeak = max - min;
        lastZcr = (double) zc / s.length;

        switch (w.getType()) {
            case ECG:
                return classifyEcg(w);
            case PPG:
                return classifyPpg(w);
            case EEG:
                return classifyEeg(w);
            default:
                return null;
        }
    }

    private AdverseEventAlertSignal classifyEcg(WaveformSignal w) {
        // Heuristic: extremely flat (asystole) or very high ZCR (VF-like).
        if (lastPeakToPeak < 0.05) {
            return alert(AlertSeverity.CRITICAL, "ECG_ASYSTOLE_LIKELY", w.getPatientId(),
                    "peakToPeak=" + lastPeakToPeak);
        }
        if (lastZcr > 0.4) {
            return alert(AlertSeverity.CRITICAL, "ECG_VF_SUSPECTED", w.getPatientId(),
                    "zcr=" + lastZcr);
        }
        if (lastZcr > 0.25) {
            return alert(AlertSeverity.URGENT, "ECG_ARRHYTHMIA_SUSPECTED", w.getPatientId(),
                    "zcr=" + lastZcr);
        }
        return null;
    }

    private AdverseEventAlertSignal classifyPpg(WaveformSignal w) {
        if (lastPeakToPeak < 0.02) {
            return alert(AlertSeverity.URGENT, "PPG_LOW_PERFUSION", w.getPatientId(),
                    "peakToPeak=" + lastPeakToPeak);
        }
        return null;
    }

    private AdverseEventAlertSignal classifyEeg(WaveformSignal w) {
        if (lastRms < 0.01) {
            return alert(AlertSeverity.WARNING, "EEG_BURST_SUPPRESSION", w.getPatientId(),
                    "rms=" + lastRms);
        }
        return null;
    }

    private AdverseEventAlertSignal alert(AlertSeverity sev, String code, String patient, String detail) {
        AdverseEventAlertSignal a = new AdverseEventAlertSignal(sev, code, patient);
        a.setDetail(detail);
        return a;
    }

    public double getLastRms() { return lastRms; }
    public double getLastZcr() { return lastZcr; }
    public double getLastPeakToPeak() { return lastPeakToPeak; }
    public long getAnalysedCount() { return analysed; }
}
