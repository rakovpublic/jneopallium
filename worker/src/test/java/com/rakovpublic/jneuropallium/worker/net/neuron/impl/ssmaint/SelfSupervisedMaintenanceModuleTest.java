/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.ssmaint;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.AssetTelemetrySignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.HealthHypothesisSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.MaintenanceAdvisorySignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.OperatorFeedbackSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.ReconResidualSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.ThresholdUpdateSignal;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.ssmaint.AdvisoryGateProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.ssmaint.CrossSensorReconstructionProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.ssmaint.FeedbackAdaptationProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.ssmaint.MaintenanceHypothesisProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.ssmaint.ThresholdUpdateProcessor;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Behavioural tests for the label-free, continuously-learning maintenance model.
 * The synthetic data here uses no fault labels; where a "fault" appears it only
 * shapes the sensor residuals, exactly as unlabeled telemetry would.
 */
class SelfSupervisedMaintenanceModuleTest {

    // ---------- enums / config ----------

    @Test
    void faultFamily_fromKey() {
        assertEquals(FaultFamily.CAVITATION, FaultFamily.fromKey("cavitation"));
        assertEquals(FaultFamily.UNKNOWN_ANOMALY, FaultFamily.fromKey("nope"));
        assertEquals(6, FaultFamily.values().length);
    }

    @Test
    void config_advisoryOnlyIsInvariant() {
        SsMaintConfig c = new SsMaintConfig();
        assertTrue(c.isLabelFree());
        assertTrue(c.isContinuousLearning());
        assertThrows(IllegalArgumentException.class, () -> c.setAdvisoryOnly(false));
        assertTrue(c.isAdvisoryOnly());
    }

    // ---------- ProcessingFrequency ----------

    @Test
    void signals_frequencies() {
        assertEquals(1L, AssetTelemetrySignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(1, AssetTelemetrySignal.PROCESSING_FREQUENCY.getLoop());
        assertEquals(1L, ReconResidualSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(2L, HealthHypothesisSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(10L, OperatorFeedbackSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(10L, ThresholdUpdateSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(2L, MaintenanceAdvisorySignal.PROCESSING_FREQUENCY.getEpoch());
    }

    // ---------- self-supervised reconstruction ----------

    private static CrossSensorReconstructionNeuron reconstructor() {
        CrossSensorReconstructionNeuron rec = new CrossSensorReconstructionNeuron();
        rec.setSensorOrder(Arrays.asList("vibration_rms", "bearing_temp", "pump_power"));
        rec.setRegimeMeans(new double[][]{{0, 0, 0}});
        rec.setRegimeStds(new double[][]{{1, 1, 1}});
        rec.setCrossWeights(new double[][]{{0, 0, 0}, {0, 0, 0}, {0, 0, 0}}); // pred=0 -> residual=z
        rec.setDomainShiftZ(6.0);
        return rec;
    }

    private static AssetTelemetrySignal frame(double vib, double bearing, double power, long ts) {
        Map<String, Double> s = new LinkedHashMap<>();
        s.put("vibration_rms", vib);
        s.put("bearing_temp", bearing);
        s.put("pump_power", power);
        return new AssetTelemetrySignal("P1", 0, s, ts);
    }

    @Test
    void reconstruction_healthyLowAnomalyHigh() {
        CrossSensorReconstructionNeuron rec = reconstructor();
        assertTrue(rec.reconstruct(frame(0.1, -0.1, 0.0, 1L)).getTotal() < 0.05);

        ReconResidualSignal anomaly = rec.reconstruct(frame(7.0, 0.0, 0.0, 2L));
        assertTrue(anomaly.getTotal() > 5.0);
        assertTrue(anomaly.getResiduals().get("vibration_rms") > 6.0);
        assertTrue(anomaly.getDomainShift() > 0.3, "z beyond envelope flags domain shift");
    }

    // ---------- hypothesis fusion ----------

    private static ReconResidualSignal residual(double total, double vib, double bearing, long ts) {
        Map<String, Double> r = new LinkedHashMap<>();
        r.put("vibration_rms", vib);
        r.put("bearing_temp", bearing);
        return new ReconResidualSignal("P1", 0, total, 0.0, r, ts);
    }

    @Test
    void hypothesis_accumulatesEvidenceOnRisingTrend() {
        MaintenanceHypothesisNeuron hyp = new MaintenanceHypothesisNeuron();
        hyp.setBaseline(0.0, 1.0, 8.0);
        HealthHypothesisSignal last = null;
        for (int i = 0; i < 400; i++) {
            last = hyp.assess(residual(0.01 * i, 2.0, 1.5, i));   // persistent upward ramp
        }
        assertTrue(hyp.evidenceFor("P1") > 0.3);
        assertTrue(last.getSeverity() > 0.6);
        assertEquals("bearing_damage", last.getFaultFamily());
        assertTrue(last.getLeadTimeTicks() >= 0);
    }

    @Test
    void hypothesis_healthyStaysQuiet() {
        MaintenanceHypothesisNeuron hyp = new MaintenanceHypothesisNeuron();
        hyp.setBaseline(0.0, 1.0, 8.0);
        for (int i = 0; i < 400; i++) {
            hyp.assess(residual(0.02, 0.1, -0.05, i));
        }
        assertTrue(hyp.evidenceFor("P1") < 0.2);
    }

    // ---------- continuous feedback learning ----------

    @Test
    void feedback_falsePositiveRaises_confirmedRelaxes() {
        FeedbackAdaptationNeuron fb = new FeedbackAdaptationNeuron();
        fb.setBaseThreshold("bearing_damage", 1.0);
        fb.setRateLimitTicks(0L);
        double t0 = fb.currentThreshold("bearing_damage");
        for (int i = 0; i < 5; i++) {
            fb.onFeedback(new OperatorFeedbackSignal("P1", "bearing_damage", false, 0.0, "op", 100L + i));
        }
        double raised = fb.currentThreshold("bearing_damage");
        assertTrue(raised > t0);
        for (int i = 0; i < 3; i++) {
            fb.onFeedback(new OperatorFeedbackSignal("P1", "bearing_damage", true, 0.0, "op", 200L + i));
        }
        assertTrue(fb.currentThreshold("bearing_damage") < raised);
    }

    @Test
    void feedback_frozenDuringDomainShift() {
        FeedbackAdaptationNeuron fb = new FeedbackAdaptationNeuron();
        double before = fb.currentThreshold("cavitation");
        ThresholdUpdateSignal none =
                fb.onFeedback(new OperatorFeedbackSignal("P1", "cavitation", false, 0.9, "op", 500L));
        assertNull(none);
        assertEquals(before, fb.currentThreshold("cavitation"), 1e-9);
    }

    @Test
    void feedback_firstEventIsNotSuppressed() {
        // Regression guard: a large epoch-style timestamp must not overflow the
        // rate-limit sentinel and swallow the first update.
        FeedbackAdaptationNeuron fb = new FeedbackAdaptationNeuron();
        fb.setBaseThreshold("energy", 1.0);
        ThresholdUpdateSignal u =
                fb.onFeedback(new OperatorFeedbackSignal("P1", "energy", false, 0.0, "op", 1_700_000_000_000L));
        assertNotNull(u);
        assertTrue(fb.currentThreshold("energy") > 1.0);
    }

    // ---------- read-only advisory gate ----------

    private static HealthHypothesisSignal hypothesis(double evidence, long ts) {
        return new HealthHypothesisSignal("P1", "bearing_damage", 2.0, evidence, 40, 0.3, 0.0, ts);
    }

    @Test
    void gate_thresholdDedupAndLiveUpdate() {
        SsAdvisoryGateNeuron gate = new SsAdvisoryGateNeuron();
        gate.setDefaultThreshold(1.0);
        gate.setDeduplicationTicks(60L);

        assertNull(gate.gate(hypothesis(0.5, 1000L)), "below threshold -> no advisory");

        MaintenanceAdvisorySignal adv = gate.gate(hypothesis(1.5, 1000L));
        assertNotNull(adv);
        assertTrue(adv.isAdvisoryOnly());

        assertNull(gate.gate(hypothesis(1.5, 1010L)), "dedup window suppresses re-fire");

        gate.onThresholdUpdate(new ThresholdUpdateSignal("bearing_damage", 3.0, 2.0, 1100L));
        assertNull(gate.gate(hypothesis(2.5, 2000L)), "live threshold update raises the bar");
    }

    @Test
    void gate_firstAdvisoryNotSuppressedByEpochTimestamp() {
        SsAdvisoryGateNeuron gate = new SsAdvisoryGateNeuron();
        gate.setDefaultThreshold(1.0);
        assertNotNull(gate.gate(hypothesis(1.5, 1_700_000_000_000L)));
    }

    // ---------- processors ----------

    private static void assertInterfaceTyped(ISignalProcessor<?, ?> p) {
        assertTrue(p.getNeuronClass().isInterface(),
                p.getClass().getSimpleName() + " must target an interface");
        assertNotNull(p.getSignalClass());
        assertNotNull(p.getDescription());
        assertFalse(p.hasMerger());
    }

    @Test
    void processors_allInterfaceTyped() {
        assertInterfaceTyped(new CrossSensorReconstructionProcessor());
        assertInterfaceTyped(new MaintenanceHypothesisProcessor());
        assertInterfaceTyped(new FeedbackAdaptationProcessor());
        assertInterfaceTyped(new AdvisoryGateProcessor());
        assertInterfaceTyped(new ThresholdUpdateProcessor());
    }

    @Test
    void reconstructionProcessor_emitsResidual() {
        CrossSensorReconstructionProcessor p = new CrossSensorReconstructionProcessor();
        List<ISignal> out = p.process(frame(7.0, 0.0, 0.0, 1L), reconstructor());
        assertEquals(1, out.size());
        assertTrue(out.get(0) instanceof ReconResidualSignal);
    }

    @Test
    void thresholdUpdateProcessor_mutatesGateInPlace() {
        SsAdvisoryGateNeuron gate = new SsAdvisoryGateNeuron();
        gate.setDefaultThreshold(1.0);
        new ThresholdUpdateProcessor().process(
                new ThresholdUpdateSignal("bearing_damage", 5.0, 4.0, 10L), gate);
        assertEquals(5.0, gate.thresholdFor("bearing_damage"), 1e-9);
    }
}
