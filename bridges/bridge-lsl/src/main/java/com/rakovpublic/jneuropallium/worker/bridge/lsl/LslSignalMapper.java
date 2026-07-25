/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.lsl;

import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.affect.AppraisalSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.affect.InteroceptiveSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.CalibrationSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.ECoGSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.LFPSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.NeuralSpikeSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.ThermalSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.ProprioceptiveSignal;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Pure functions converting LSL chunks to the existing typed Jneopallium
 * signals (05-LSL.md §5 mapping table). Stateless; one instance per
 * {@link LslClientService}.
 *
 * <p>05-LSL.md §10 R4 — the mapper strips subject identifiers (any value
 * that is not on the configured channel-label list) before constructing
 * a signal. Channel labels themselves are operator-controlled, not
 * subject-controlled, so they are safe to keep.
 */
public final class LslSignalMapper {

    /**
     * EEG → LFP. The LSL EEG payload is one electrode-voltage value per
     * channel (in µV); for each configured channel we compute very
     * coarse per-band "powers" by binning — the fast-path bridge does not
     * own an FFT, the BCI signal-processor pipeline does. The mapper
     * therefore emits per-channel {@link LFPSignal}s with the raw voltage
     * mirrored into every band slot, so the downstream
     * {@code SeizureAssessmentProcessor} can apply its own filter without
     * the bridge pretending to deliver pre-binned power.
     */
    public List<IInputSignal> toLfp(LslStreamBinding b, double[] sample,
                                    long timestampNs, String inputName) {
        if (sample == null || b.channelIndices().length == 0) return List.of();
        int[] idx = b.channelIndices();
        List<String> labels = b.channelLabels();
        List<IInputSignal> out = new ArrayList<>(idx.length);
        for (int i = 0; i < idx.length; i++) {
            int chIdx = idx[i];
            if (chIdx < 0 || chIdx >= sample.length) continue;
            double v = sample[chIdx];
            double[] bands = new double[]{v, v, v, v, v, v};
            LFPSignal s = new LFPSignal(channelHashOf(labels, i), bands, timestampNs);
            s.setInputName(inputName);
            out.add(s);
        }
        return out;
    }

    /** ECoG → ECoG. */
    public List<IInputSignal> toEcog(LslStreamBinding b, double[] sample,
                                     long timestampNs, String inputName) {
        if (sample == null || b.channelIndices().length == 0) return List.of();
        int[] idx = b.channelIndices();
        List<String> labels = b.channelLabels();
        List<IInputSignal> out = new ArrayList<>(idx.length);
        for (int i = 0; i < idx.length; i++) {
            int chIdx = idx[i];
            if (chIdx < 0 || chIdx >= sample.length) continue;
            ECoGSignal s = new ECoGSignal(channelHashOf(labels, i), sample[chIdx], timestampNs);
            s.setInputName(inputName);
            out.add(s);
        }
        return out;
    }

    /**
     * Spikes → NeuralSpike. Convention: payload is
     * {@code [unitId, ch0Wave, ch1Wave, ...]} — the unit id occupies the
     * first column, the rest is the waveform snippet.
     */
    public List<IInputSignal> toSpike(LslStreamBinding b, double[] sample,
                                      long timestampNs, String inputName) {
        if (sample == null || sample.length == 0) return List.of();
        int unitId = (int) Math.round(sample[0]);
        double[] snippet = new double[sample.length - 1];
        if (snippet.length > 0) System.arraycopy(sample, 1, snippet, 0, snippet.length);
        NeuralSpikeSignal s = new NeuralSpikeSignal(0, unitId, snippet, timestampNs);
        s.setInputName(inputName);
        return List.of(s);
    }

    /**
     * EMG → Proprioceptive. One {@link ProprioceptiveSignal} per sample
     * carrying every configured channel's value in {@code jointStates}.
     */
    public List<IInputSignal> toProprioceptive(LslStreamBinding b, double[] sample,
                                               long timestampMs, String inputName) {
        if (sample == null) return List.of();
        int[] idx = b.channelIndices();
        if (idx.length == 0) return List.of();
        double[] joints = new double[idx.length];
        for (int i = 0; i < idx.length; i++) {
            int j = idx[i];
            joints[i] = (j >= 0 && j < sample.length) ? sample[j] : 0.0;
        }
        ProprioceptiveSignal s = new ProprioceptiveSignal(0, joints, timestampMs);
        s.setInputName(inputName);
        return List.of(s);
    }

    /**
     * HRV / GSR → Interoceptive. Convention: {@code energyBudget} = first
     * channel value (RR-interval ms or skin conductance µS),
     * {@code homeostaticError} = mean absolute deviation across configured
     * channels, {@code painMagnitude} = 0 (the bridge does not infer pain).
     */
    public List<IInputSignal> toInteroceptive(LslStreamBinding b, double[] sample,
                                              String inputName) {
        if (sample == null || b.channelIndices().length == 0) return List.of();
        int[] idx = b.channelIndices();
        double primary = (idx[0] >= 0 && idx[0] < sample.length) ? sample[idx[0]] : 0.0;
        double mean = 0.0;
        int n = 0;
        for (int j : idx) if (j >= 0 && j < sample.length) { mean += sample[j]; n++; }
        if (n > 0) mean /= n;
        double mad = 0.0;
        for (int j : idx) if (j >= 0 && j < sample.length) mad += Math.abs(sample[j] - mean);
        if (n > 0) mad /= n;
        InteroceptiveSignal s = new InteroceptiveSignal(primary, mad, 0.0, b.streamType());
        s.setInputName(inputName);
        return List.of(s);
    }

    /**
     * Eye → Appraisal. Convention: gaze x → {@code goalDelta}, gaze y →
     * {@code novelty}, pupil diameter → {@code controllability} (proxy for
     * arousal / engagement). Channel order is taken from the configured
     * channel list.
     */
    public List<IInputSignal> toAppraisal(LslStreamBinding b, double[] sample,
                                          String inputName) {
        if (sample == null || b.channelIndices().length == 0) return List.of();
        int[] idx = b.channelIndices();
        double x = idx.length > 0 && idx[0] >= 0 && idx[0] < sample.length ? sample[idx[0]] : 0.0;
        double y = idx.length > 1 && idx[1] >= 0 && idx[1] < sample.length ? sample[idx[1]] : 0.0;
        double diam = idx.length > 2 && idx[2] >= 0 && idx[2] < sample.length ? sample[idx[2]] : 0.0;
        AppraisalSignal s = new AppraisalSignal(x, y, diam);
        s.setInputName(inputName);
        return List.of(s);
    }

    /** Temperature → Thermal. */
    public List<IInputSignal> toThermal(LslStreamBinding b, double[] sample,
                                        String inputName) {
        if (sample == null || b.channelIndices().length == 0) return List.of();
        int[] idx = b.channelIndices();
        int j = idx[0];
        double t = (j >= 0 && j < sample.length) ? sample[j] : 0.0;
        ThermalSignal s = new ThermalSignal(0, t, 0.0);
        s.setInputName(inputName);
        return List.of(s);
    }

    /**
     * Marker stream → Calibration when the marker text matches the
     * configured cue regex (05-LSL.md §5). Subject identifiers are
     * stripped: the {@code sessionId} becomes a stable hash of the
     * matched marker, never the raw string, mitigating §10 R4.
     */
    public CalibrationSignal toCalibration(LslStreamBinding b, String marker,
                                           String inputName) {
        if (marker == null || b.calibrationCueRegex() == null) return null;
        if (!Pattern.compile(b.calibrationCueRegex()).matcher(marker).find()) return null;
        // Pseudonymise: the session id is the marker's hash, not its value.
        String pseudo = "MARKER-" + Integer.toHexString(marker.hashCode());
        CalibrationSignal s = new CalibrationSignal(pseudo, null, 1.0);
        s.setInputName(inputName);
        return s;
    }

    /** Stable channel identifier — labels are pre-validated, so a hash is fine. */
    private static int channelHashOf(List<String> labels, int i) {
        if (labels == null || i < 0 || i >= labels.size()) return i;
        String label = labels.get(i);
        return label == null ? i : (label.hashCode() & 0x7fffffff);
    }
}
