/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.AgencyLossSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.CalibrationSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.ChargeAccumulationSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.DriftEstimateSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.IntentSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.LFPSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.NeuralSpikeSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.SeizureRiskSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.SensoryFeedbackSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.StimulationCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.ThermalSignal;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BciModuleTest {

    // ---------- signals: ProcessingFrequency + copy ----------

    @Test
    void neuralSpikeSignal_frequency() {
        assertEquals(1L, NeuralSpikeSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(1, NeuralSpikeSignal.PROCESSING_FREQUENCY.getLoop());
    }

    @Test
    void lfpSignal_sixBands() {
        double[] p = new double[6];
        p[LFPSignal.HIGH_GAMMA] = 0.5;
        LFPSignal s = new LFPSignal(3, p, 1_000_000L);
        assertEquals(0.5, s.getBandPowers()[LFPSignal.HIGH_GAMMA], 1e-9);
        assertEquals(3, s.getChannelId());
    }

    @Test
    void stimulationCommandSignal_chargePerPhaseInMicrocoulombs() {
        StimulationCommandSignal s = new StimulationCommandSignal(0, 100, 200, 50, 1, PolarityPattern.CATHODIC_FIRST_BIPHASIC);
        assertEquals(0.02, s.chargePerPhaseUC(), 1e-9);
    }

    @Test
    void chargeAccumulationSignal_copyPreservesFields() {
        ChargeAccumulationSignal s = new ChargeAccumulationSignal(7, 0.003, 0.5);
        ChargeAccumulationSignal c = (ChargeAccumulationSignal) s.copySignal();
        assertEquals(7, c.getElectrodeId());
        assertEquals(0.003, c.getNetChargeUC(), 1e-12);
    }

    @Test
    void agencyLossSignal_roundtrips() {
        AgencyLossSignal s = new AgencyLossSignal(0.42, "vision");
        AgencyLossSignal c = (AgencyLossSignal) s.copySignal();
        assertEquals(0.42, c.getMismatchMagnitude(), 1e-9);
        assertEquals("vision", c.getModality());
    }

    // ---------- Layer 0 ----------

    @Test
    void spikeRecording_detectsThresholdCrossing() {
        SpikeRecordingNeuron rec = new SpikeRecordingNeuron();
        rec.setThresholdUV(80);
        rec.setChannelId(2);
        double[] trace = {10, 20, 30, 90, 120, 40};
        NeuralSpikeSignal s = rec.detect(trace, 0L, 1_000L);
        assertNotNull(s);
        assertEquals(2, s.getChannelId());
    }

    @Test
    void spikeSorting_assignsUnits() {
        SpikeSortingNeuron sorter = new SpikeSortingNeuron();
        NeuralSpikeSignal a = new NeuralSpikeSignal(0, -1, new double[]{1, 2, 3, 2, 1}, 0L);
        NeuralSpikeSignal b = new NeuralSpikeSignal(0, -1, new double[]{1, 2, 3, 2, 1}, 1L);
        NeuralSpikeSignal c = new NeuralSpikeSignal(0, -1, new double[]{-1, -2, -3, -2, -1}, 2L);
        int ua = sorter.sort(a);
        int ub = sorter.sort(b);
        int uc = sorter.sort(c);
        assertEquals(ua, ub);
        assertNotEquals(ua, uc);
    }

    @Test
    void lfpExtraction_producesSixBands() {
        LFPExtractionNeuron lfp = new LFPExtractionNeuron();
        double[] window = new double[64];
        for (int i = 0; i < window.length; i++) window[i] = Math.sin(i * 0.1) * 50;
        LFPSignal s = lfp.extract(1, window, 0L);
        assertEquals(6, s.getBandPowers().length);
        assertTrue(s.getBandPowers()[LFPSignal.DELTA] > 0);
    }

    @Test
    void artefactRejection_masksChannelOverLimit() {
        ArtefactRejectionNeuron ar = new ArtefactRejectionNeuron();
        ar.setAbsAmplitudeLimitUV(400);
        double[] clean = {10, 20, 30};
        double[] dirty = {10, 500, 30};
        assertFalse(ar.check(1, clean));
        assertTrue(ar.check(1, dirty));
        assertTrue(ar.isMasked(1));
    }

    // ---------- Layer 1 ----------

    @Test
    void firingRateEstimator_updatesRatesOverTime() {
        FiringRateEstimatorNeuron fr = new FiringRateEstimatorNeuron();
        fr.setTauSeconds(0.05);
        fr.onSpike(0, 1_000_000L);
        double r1 = fr.onSpike(0, 2_000_000L);
        double r2 = fr.onSpike(0, 3_000_000L);
        assertTrue(r1 > 0);
        assertTrue(r2 > 0);
    }

    @Test
    void populationVector_alignsWithTunedDirections() {
        PopulationVectorNeuron pv = new PopulationVectorNeuron();
        pv.tunePreferredDirection(0, new double[]{1, 0, 0});
        pv.tunePreferredDirection(1, new double[]{0, 1, 0});
        Map<Integer, Double> rates = new HashMap<>();
        rates.put(0, 10.0);
        rates.put(1, 1.0);
        double[] v = pv.decode(rates);
        assertTrue(v[0] > v[1]);
    }

    @Test
    void kalmanDecoder_convergesTowardObservation() {
        KalmanDecoderNeuron k = new KalmanDecoderNeuron();
        double pos = 0;
        for (int i = 0; i < 200; i++) pos = k.step(10.0);
        assertTrue(Math.abs(pos - 10.0) < 2.0, "Kalman should track steady 10; got " + pos);
    }

    @Test
    void latentDynamics_producesBoundedLatent() {
        LatentDynamicsNeuron ld = new LatentDynamicsNeuron();
        double[] r = new double[16];
        for (int i = 0; i < 16; i++) r[i] = 1.0;
        double[] latent = null;
        for (int i = 0; i < 50; i++) latent = ld.step(r);
        assertNotNull(latent);
        for (double v : latent) assertTrue(Double.isFinite(v));
    }

    @Test
    void speechPhonemeDecoder_picksNearestCentroid() {
        SpeechPhonemeDecoderNeuron dec = new SpeechPhonemeDecoderNeuron();
        dec.trainPhoneme("a", new double[]{1, 0});
        dec.trainPhoneme("i", new double[]{0, 1});
        assertEquals("a", dec.classify(new double[]{0.9, 0.1}));
        assertEquals("i", dec.classify(new double[]{0.1, 0.9}));
    }

    // ---------- Layer 2 ----------

    @Test
    void intentFusion_confidenceWeightedBlend() {
        IntentFusionNeuron fuse = new IntentFusionNeuron();
        IntentSignal a = new IntentSignal(IntentKind.REACH, new double[]{1, 0}, 0.9);
        IntentSignal b = new IntentSignal(IntentKind.REACH, new double[]{0, 1}, 0.3);
        IntentSignal out = fuse.fuse(a, b);
        assertEquals(IntentKind.REACH, out.getKind());
        assertTrue(out.getParameters()[0] > out.getParameters()[1]);
    }

    @Test
    void userState_classifiesDistressFirst() {
        UserStateNeuron us = new UserStateNeuron();
        assertEquals(UserStateNeuron.State.DISTRESSED, us.classify(0.9, 0.9, 0.9));
        assertEquals(UserStateNeuron.State.ALERT, us.classify(0.1, 0.1, 0.1));
    }

    // ---------- Layer 3 ----------

    @Test
    void decoderWeight_updatesWithHebbianRule() {
        DecoderWeightNeuron dw = new DecoderWeightNeuron();
        dw.init(3);
        double[] rates = {1, 0, 0};
        for (int i = 0; i < 100; i++) dw.update(rates, 1.0);
        double[] w = dw.getWeights();
        assertTrue(w[0] > w[1] && w[0] > w[2]);
    }

    @Test
    void driftTracker_flagsHighDrift() {
        DriftTrackerNeuron dt = new DriftTrackerNeuron();
        dt.setDriftTolerance(0.2);
        for (int i = 0; i < 50; i++) dt.observe(3, 0.6, 5.0);
        assertTrue(dt.needsRecalibration(3));
    }

    @Test
    void personalMotorLexicon_matchesStoredGesture() {
        PersonalMotorLexiconNeuron lex = new PersonalMotorLexiconNeuron();
        lex.register("scroll", new double[]{1, 1, 0});
        lex.register("click", new double[]{0, 0, 1});
        assertEquals("scroll", lex.match(new double[]{1, 0.9, 0}));
    }

    // ---------- Layer 4 ----------

    @Test
    void prostheticPlanner_clipsToJointLimits() {
        ProstheticPlanningNeuron pp = new ProstheticPlanningNeuron();
        pp.setDof(2);
        pp.setJointLimits(0, -0.5, 0.5);
        pp.setJointLimits(1, -0.5, 0.5);
        double[] out = pp.step(new double[]{10.0, 10.0}, 0.2);
        for (double v : out) assertTrue(v <= 0.5 && v >= -0.5);
    }

    @Test
    void gripSelection_picksGripByGeometry() {
        GripSelectionNeuron gs = new GripSelectionNeuron();
        assertEquals(GripSelectionNeuron.GripType.PINCH, gs.select(0.005, 0.3, true));
        assertEquals(GripSelectionNeuron.GripType.POWER, gs.select(0.15, 0.2, false));
    }

    // ---------- Layer 5 — safety-critical ----------

    @Test
    void stimulationSafetyGate_vetoesShannonViolation() {
        StimulationSafetyGateNeuron gate = new StimulationSafetyGateNeuron();
        gate.setElectrodeArea(0, 1e-5);  // 0.00001 cm²
        // chargePerPhase = 100 µA * 200 µs / 1e6 = 0.02 µC → density = 2000 µC/cm² >> 0.5
        StimulationCommandSignal bad = new StimulationCommandSignal(0, 100, 200, 50, 1, PolarityPattern.CATHODIC_FIRST_BIPHASIC);
        assertEquals("charge_density_exceeds_shannon", gate.veto(bad, 0L));
    }

    @Test
    void stimulationSafetyGate_vetoesOverFrequency() {
        StimulationSafetyGateNeuron gate = new StimulationSafetyGateNeuron();
        gate.setElectrodeArea(0, 1.0);
        StimulationCommandSignal bad = new StimulationCommandSignal(0, 1, 10, 500, 1, PolarityPattern.CATHODIC_FIRST_BIPHASIC);
        assertEquals("frequency_exceeds_max", gate.veto(bad, 0L));
    }

    @Test
    void stimulationSafetyGate_vetoesSeizureLockout() {
        StimulationSafetyGateNeuron gate = new StimulationSafetyGateNeuron();
        gate.setElectrodeArea(0, 1.0);
        gate.triggerSeizureLockout(1000L);
        StimulationCommandSignal ok = new StimulationCommandSignal(0, 1, 10, 50, 1, PolarityPattern.CATHODIC_FIRST_BIPHASIC);
        assertEquals("seizure_lockout", gate.veto(ok, 0L));
        assertNull(gate.veto(ok, 2000L));  // past lockout
    }

    @Test
    void stimulationSafetyGate_passesSafeCommand() {
        StimulationSafetyGateNeuron gate = new StimulationSafetyGateNeuron();
        gate.setElectrodeArea(0, 1.0);  // big area → low density
        StimulationCommandSignal safe = new StimulationCommandSignal(0, 10, 100, 50, 1, PolarityPattern.CATHODIC_FIRST_BIPHASIC);
        assertNull(gate.veto(safe, 0L));
    }

    @Test
    void chargeBalance_flagsDcBuildup() {
        ChargeBalanceNeuron cb = new ChargeBalanceNeuron();
        cb.setDcToleranceUC(1e-3);
        StimulationCommandSignal cmd = new StimulationCommandSignal(5, 100, 200, 50, 100, PolarityPattern.BIPHASIC_ASYMMETRIC);
        for (int i = 0; i < 5; i++) cb.accumulate(cmd, 0.01);
        assertTrue(cb.exceedsDc(5), "DC build-up should exceed tolerance; net=" + cb.getNetCharge(5));
    }

    @Test
    void seizureWatchdog_flagsHighGammaRisk() {
        SeizureWatchdogNeuron sw = new SeizureWatchdogNeuron();
        double[] p = new double[6];
        p[LFPSignal.HIGH_GAMMA] = 10.0;
        LFPSignal lfp = new LFPSignal(1, p, 0L);
        SeizureRiskSignal r = sw.assess(lfp, 1);
        assertTrue(r.getRisk() > 0.5);
        assertTrue(sw.shouldTriggerLockout(r.getRisk()));
        assertEquals(SeizureMarker.HIGH_FREQUENCY_OSCILLATION, r.getMarker());
    }

    @Test
    void actuator_recordsDispatch() {
        ActuatorNeuron act = new ActuatorNeuron();
        StimulationCommandSignal cmd = new StimulationCommandSignal(0, 1, 10, 50, 1, PolarityPattern.CATHODIC_FIRST_BIPHASIC);
        assertTrue(act.dispatch(cmd, 42L));
        assertEquals(1, act.getDispatchedCount());
        assertEquals(42L, act.getLastDispatchTick());
    }

    // ---------- Layer 7 ----------

    @Test
    void thermalMonitor_triggersShutdownAt2C() {
        ThermalMonitorNeuron tm = new ThermalMonitorNeuron();
        tm.observe(new ThermalSignal(0, 38.1, 1.1));  // cool-down
        assertTrue(tm.isCoolDown());
        assertFalse(tm.isShutdown());
        tm.observe(new ThermalSignal(0, 39.2, 2.2));  // shutdown
        assertTrue(tm.isShutdown());
    }

    @Test
    void powerBudget_reducesModeOnDrain() {
        PowerBudgetNeuron pb = new PowerBudgetNeuron();
        pb.setCapacityMAh(100);
        pb.setRemainingMAh(100);
        assertEquals(PowerBudgetNeuron.PowerMode.NORMAL, pb.getMode());
        pb.drain(75);
        assertEquals(PowerBudgetNeuron.PowerMode.CONSERVE, pb.getMode());
        pb.drain(20);
        assertEquals(PowerBudgetNeuron.PowerMode.EMERGENCY, pb.getMode());
        assertFalse(pb.stimAllowed());
    }

    @Test
    void calibrationScheduler_emitsOnDriftTrigger() {
        CalibrationSchedulerNeuron sched = new CalibrationSchedulerNeuron();
        sched.setMinIntervalTicks(0);
        sched.setMaxIntervalTicks(1_000_000);
        sched.setDriftTrigger(0.2);
        CalibrationSignal c = sched.evaluate(10_000L, 0.4, 0.1, true);
        assertNotNull(c);
        assertEquals(CalibrationTarget.CHANNEL_SELECTION, c.getTarget());
    }

    @Test
    void calibrationScheduler_skipsIfNoTrigger() {
        CalibrationSchedulerNeuron sched = new CalibrationSchedulerNeuron();
        sched.setMinIntervalTicks(1_000_000);
        sched.setMaxIntervalTicks(10_000_000);
        assertNull(sched.evaluate(500L, 0.1, 0.1, true));
    }

    // ---------- enums + config ----------

    @Test
    void bciConfig_defaultsMatchSpec() {
        BciConfig c = new BciConfig();
        assertFalse(c.isEnabled());
        assertEquals(0.5, c.getMaxChargeDensityUCcm2(), 1e-9);
        assertEquals(1e-3, c.getNetDcToleranceUC(), 1e-12);
        assertEquals(300.0, c.getMaxFrequencyHz(), 1e-9);
        assertEquals(300.0, c.getMaxPulseWidthUS(), 1e-9);
        assertEquals(1.0, c.getThermalCoolDownDeltaC(), 1e-9);
        assertEquals(2.0, c.getThermalShutdownDeltaC(), 1e-9);
        assertEquals(0.8, c.getSeizureRiskThreshold(), 1e-9);
        assertEquals(500.0, c.getBatteryCapacityMAh(), 1e-9);
    }

    @Test
    void enums_haveAllLabels() {
        assertEquals(8, IntentKind.values().length);
        assertEquals(4, PolarityPattern.values().length);
        assertEquals(4, FeedbackModality.values().length);
        assertEquals(5, CalibrationTarget.values().length);
        assertEquals(6, SeizureMarker.values().length);
    }

    // ---------- signals: sensory + drift + thermal ----------

    @Test
    void sensoryFeedbackSignal_roundtrips() {
        SensoryFeedbackSignal s = new SensoryFeedbackSignal(FeedbackModality.TACTILE, 7, 0.5, 0.1);
        SensoryFeedbackSignal c = (SensoryFeedbackSignal) s.copySignal();
        assertEquals(FeedbackModality.TACTILE, c.getModality());
        assertEquals(7, c.getAfferentId());
    }

    @Test
    void driftEstimateSignal_roundtrips() {
        DriftEstimateSignal s = new DriftEstimateSignal(3, 0.2, 4.0);
        DriftEstimateSignal c = (DriftEstimateSignal) s.copySignal();
        assertEquals(3, c.getChannelId());
        assertEquals(0.2, c.getDrift(), 1e-9);
    }

    @Test
    void thermalSignal_roundtrips() {
        ThermalSignal s = new ThermalSignal(2, 37.5, 0.5);
        ThermalSignal c = (ThermalSignal) s.copySignal();
        assertEquals(2, c.getSensorId());
        assertEquals(0.5, c.getDeltaFromBaseline(), 1e-9);
    }
}
