/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.ActuatorCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.BatchStateSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.DegradationSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.EfficiencySignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.InterlockSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MaintenanceWindowSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.OperatorOverrideSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.SetpointSignal;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial.ActuatorDispatchProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial.ActuatorSafetyGateProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial.AlarmAggregationProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial.BatchModeProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial.DegradationSchedulingProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial.EfficiencyOptimiserProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial.InterlockModeProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial.MaintenanceWindowSchedulingProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial.MeasurementInterlockProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial.MeasurementOscillationProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial.MeasurementPIDProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial.MeasurementValidationProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial.OperatorOverrideProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial.SetpointPIDProcessor;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IndustrialModuleTest {

    // ---------- enums ----------

    @Test
    void enums_cardinality() {
        assertEquals(3, Quality.values().length);
        assertEquals(4, AlarmPriority.values().length);
        assertEquals(7, BatchPhase.values().length);
        assertEquals(2, OverrideKind.values().length);
        assertEquals(4, PlantMode.values().length);
        assertEquals(3, SafetyMode.values().length);
        assertEquals(5, OscillationIntervention.values().length);
    }

    // ---------- ProcessingFrequency ----------

    @Test
    void signals_frequencies() {
        assertEquals(1L, MeasurementSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(1, MeasurementSignal.PROCESSING_FREQUENCY.getLoop());
        assertEquals(2L, SetpointSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(1L, ActuatorCommandSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(1L, AlarmSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(1L, InterlockSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(3L, DegradationSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(2, DegradationSignal.PROCESSING_FREQUENCY.getLoop());
        assertEquals(1L, EfficiencySignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(2, EfficiencySignal.PROCESSING_FREQUENCY.getLoop());
        assertEquals(2L, BatchStateSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(1L, OperatorOverrideSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(10L, MaintenanceWindowSignal.PROCESSING_FREQUENCY.getEpoch());
    }

    // ---------- Layer 0 ----------

    @Test
    void validator_downgradesOutOfRange() {
        MeasurementValidatorNeuron v = new MeasurementValidatorNeuron();
        v.setRange("t1", 0.0, 100.0);
        MeasurementSignal m = new MeasurementSignal("t1", 250.0, Quality.GOOD, 0L);
        MeasurementSignal r = v.validate(m);
        assertEquals(Quality.UNCERTAIN, r.getQuality());
        assertEquals(1, v.suspiciousCount());
    }

    @Test
    void validator_rateOfChange() {
        MeasurementValidatorNeuron v = new MeasurementValidatorNeuron();
        v.setMaxRateOfChange("t1", 10.0);
        v.validate(new MeasurementSignal("t1", 0.0, Quality.GOOD, 0L));
        MeasurementSignal spike = new MeasurementSignal("t1", 100.0, Quality.GOOD, 1_000L);
        assertEquals(Quality.UNCERTAIN, v.validate(spike).getQuality());
    }

    // ---------- PID ----------

    @Test
    void pid_drivesTowardSetpoint() {
        PIDNeuron pid = new PIDNeuron();
        pid.setTag("out");
        pid.setGains(1.0, 0.0, 0.0);
        pid.setOutputLimits(-10.0, 10.0);
        pid.setSetpoint(new SetpointSignal("sp", 10.0, 0.0, "ui"));
        ActuatorCommandSignal cmd = pid.step(new MeasurementSignal("pv", 0.0, Quality.GOOD, 0L), 0.1);
        assertNotNull(cmd);
        assertEquals(10.0, cmd.getTargetValue(), 1e-9);
    }

    @Test
    void pid_scaleGainsIntervention() {
        PIDNeuron pid = new PIDNeuron();
        pid.setGains(2.0, 1.0, 0.5);
        pid.scaleGains(0.5);
        assertEquals(1.0, pid.getKp(), 1e-9);
        assertEquals(0.5, pid.getKi(), 1e-9);
        assertEquals(0.25, pid.getKd(), 1e-9);
    }

    // ---------- Cascade / FeedForward ----------

    @Test
    void cascade_breaksReleasesLastGood() {
        CascadeNeuron c = new CascadeNeuron();
        SetpointSignal first = c.forward(new ActuatorCommandSignal("o", 5.0, 0.0, true), "inner");
        assertEquals(5.0, first.getSetpoint(), 1e-9);
        c.setBroken(true);
        SetpointSignal broken = c.forward(new ActuatorCommandSignal("o", 99.0, 0.0, true), "inner");
        assertSame(first, broken);
    }

    @Test
    void feedForward_addsProportionalBias() {
        FeedForwardNeuron f = new FeedForwardNeuron();
        f.setGain(2.0);
        ActuatorCommandSignal c = f.compensate(new MeasurementSignal("dist", 3.0, Quality.GOOD, 0L), "out", 1.0);
        assertEquals(7.0, c.getTargetValue(), 1e-9);
    }

    // ---------- Supervisory ----------

    @Test
    void setpointOptimiser_nudgesWithinConstraints() {
        SetpointOptimiserNeuron s = new SetpointOptimiserNeuron();
        s.setConstraint("sp", 0.0, 10.0);
        s.setStep(0.5);
        SetpointSignal out = s.optimise(new EfficiencySignal("u1", 0.9, 0.8), "sp", 9.9);
        assertEquals(10.0, out.getSetpoint(), 1e-9);
    }

    @Test
    void alarmAggregator_suppressesStanding() {
        AlarmAggregationNeuron a = new AlarmAggregationNeuron();
        a.setSuppressionWindowTicks(5_000L);
        AlarmSignal raw = new AlarmSignal(AlarmPriority.LOW, "t", "C1", 0L);
        assertNotNull(a.observe(raw));
        AlarmSignal again = new AlarmSignal(AlarmPriority.LOW, "t", "C1", 1_000L);
        assertNull(a.observe(again));
    }

    @Test
    void modeController_interlockForcesEmergency() {
        ModeControllerNeuron m = new ModeControllerNeuron();
        m.requestMode(PlantMode.NORMAL);
        m.onInterlock(new InterlockSignal("I1", true, null));
        assertEquals(PlantMode.EMERGENCY, m.getMode());
        m.requestMode(PlantMode.NORMAL);
        assertEquals(PlantMode.EMERGENCY, m.getMode(), "requestMode must be ignored while EMERGENCY");
        m.resetFromEmergency();
        assertEquals(PlantMode.SHUTDOWN, m.getMode());
    }

    // ---------- Layer 3 models ----------

    @Test
    void processModel_predict() {
        ProcessModelNeuron p = new ProcessModelNeuron();
        p.setGain("t", 2.0);
        p.setTimeConstantTicks("t", 10.0);
        p.setDeadTimeTicks("t", 2);
        double near = p.predict("t", 1.0, 1);
        double far = p.predict("t", 1.0, 50);
        assertTrue(far > near);
        assertTrue(far < 2.0 * 1.0 + 1e-9);
    }

    @Test
    void degradationModel_reducesRul() {
        DegradationModelNeuron d = new DegradationModelNeuron();
        d.seedAsset("pump-1", 1000.0);
        d.setWearPerUnit(1.0);
        d.observe("pump-1", new MeasurementSignal("wear", 5.0, Quality.GOOD, 0L));
        assertEquals(995.0, d.rulFor("pump-1"), 1e-9);
    }

    @Test
    void productQualityModel_compliance() {
        ProductQualityModelNeuron pq = new ProductQualityModelNeuron();
        pq.setTarget(100.0);
        pq.setTolerance(10.0);
        assertTrue(pq.inSpec(new double[]{98.0, 100.0, 102.0}));
        assertFalse(pq.inSpec(new double[]{200.0}));
    }

    // ---------- Layer 4 planning ----------

    @Test
    void mpc_picksBestMove() {
        ProcessModelNeuron p = new ProcessModelNeuron();
        p.setGain("t", 1.0);
        p.setTimeConstantTicks("t", 1.0);
        MPCPlanningNeuron m = new MPCPlanningNeuron();
        m.setProcessModel(p);
        ActuatorCommandSignal cmd = m.step(new SetpointSignal("t", 5.0, 0.0, "ui"), 4.0);
        assertNotNull(cmd);
        assertTrue(cmd.getTargetValue() > 4.0);
    }

    @Test
    void campaignPlanner_fifo() {
        CampaignPlanningNeuron c = new CampaignPlanningNeuron();
        c.enqueueCampaign("c1", new BatchStateSignal("b1", BatchPhase.RUNNING, null));
        c.enqueueCampaign("c2", new BatchStateSignal("b2", BatchPhase.RUNNING, null));
        assertEquals("c1", c.currentCampaign());
        c.nextPhase();
        assertEquals("c2", c.currentCampaign());
    }

    @Test
    void maintenance_scheduleLeadsToWindow() {
        MaintenanceSchedulingNeuron m = new MaintenanceSchedulingNeuron();
        MaintenanceWindowSignal w = m.schedule(new DegradationSignal("pump-1", 1.0, 0.9), 0L, 100L);
        assertNotNull(w);
        assertEquals("pump-1", w.getAssetId());
    }

    // ---------- Layer 5 response ----------

    @Test
    void safetyGate_shadowDisablesExecute() {
        SafetyGateNeuron g = new SafetyGateNeuron();
        g.setDefaultMode(SafetyMode.SHADOW);
        ActuatorCommandSignal c = g.gate(new ActuatorCommandSignal("t", 1.0, 0.0, true));
        assertFalse(c.isExecute());
    }

    @Test
    void safetyGate_autonomousExecutes() {
        SafetyGateNeuron g = new SafetyGateNeuron();
        g.setDefaultMode(SafetyMode.AUTONOMOUS);
        ActuatorCommandSignal c = g.gate(new ActuatorCommandSignal("t", 1.0, 0.0, false));
        assertTrue(c.isExecute());
    }

    @Test
    void actuator_honoursOverride() {
        ActuatorNeuron a = new ActuatorNeuron();
        a.onOverride(new OperatorOverrideSignal("t", OverrideKind.MANUAL, "op1", "hold", 7.0));
        assertTrue(a.dispatch(new ActuatorCommandSignal("t", 3.0, 0.0, true)));
        assertEquals(7.0, a.lastDispatchedValue("t"));
        a.onOverride(new OperatorOverrideSignal("t", OverrideKind.BYPASS, "op1", "lockout", 0.0));
        assertFalse(a.dispatch(new ActuatorCommandSignal("t", 3.0, 0.0, true)));
    }

    @Test
    void interlock_sealedIsImmutable() {
        InterlockNeuron i = new InterlockNeuron();
        i.addInterlock("I1", "t", 100.0, true, "act", 0.0);
        i.seal();
        assertTrue(i.isSealed());
        assertThrows(IllegalStateException.class,
                () -> i.addInterlock("I2", "t", 200.0, true, "act", 0.0));
    }

    @Test
    void interlock_tripsOnThreshold() {
        InterlockNeuron i = new InterlockNeuron();
        i.addInterlock("I1", "t", 100.0, true, "act", 0.0);
        i.seal();
        List<InterlockSignal> trips = i.evaluate(new MeasurementSignal("t", 150.0, Quality.GOOD, 0L));
        assertEquals(1, trips.size());
        assertTrue(trips.get(0).isTripped());
        assertEquals(1, i.failSafeCommands().size());
    }

    // ---------- Layer 7 ----------

    @Test
    void oscillation_detectsAndEscalates() {
        OscillationMonitorNeuron m = new OscillationMonitorNeuron();
        m.setAcfWindowTicks(16);
        // Alternating sequence: strong negative autocorrelation at lag 2
        for (int i = 0; i < 16; i++) {
            double v = (i % 2 == 0) ? 1.0 : -1.0;
            m.observe(new MeasurementSignal("t", v, Quality.GOOD, i));
        }
        double sev = m.severity("t");
        assertTrue(sev > 0.0);
    }

    @Test
    void oscillation_interventionMapping() {
        // A bit of manual severity probing across the bands.
        OscillationMonitorNeuron m = new OscillationMonitorNeuron();
        m.setAcfWindowTicks(16);
        // Gentle ripple ~ low severity
        for (int i = 0; i < 16; i++) {
            m.observe(new MeasurementSignal("t", Math.sin(i * 0.2), Quality.GOOD, i));
        }
        OscillationIntervention band = m.intervention("t");
        assertNotNull(band);
    }

    @Test
    void energyAccounting_efficiency() {
        EnergyAccountingNeuron e = new EnergyAccountingNeuron();
        e.recordEnergy("u1", 100.0);
        e.recordProduction("u1", 50.0);
        EfficiencySignal s = e.efficiencyFor("u1");
        assertEquals(0.5, s.getEfficiency(), 1e-9);
    }

    // ---------- Config ----------

    @Test
    void config_safetyModeValidation() {
        IndustrialConfig c = new IndustrialConfig();
        assertEquals(SafetyMode.ADVISORY, c.getSafetyMode());
        c.setSafetyMode(SafetyMode.AUTONOMOUS);
        assertEquals(SafetyMode.AUTONOMOUS, c.getSafetyMode());
        assertThrows(IllegalArgumentException.class, () -> c.setSafetyMode(null));
    }

    @Test
    void config_overrideAlwaysHonouredCannotBeDisabled() {
        IndustrialConfig c = new IndustrialConfig();
        assertTrue(c.isOverrideAlwaysHonoured());
        assertThrows(IllegalArgumentException.class, () -> c.setOverrideAlwaysHonoured(false));
    }

    // ---------- Processors ----------

    private static void assertInterfaceTyped(ISignalProcessor<?, ?> p) {
        assertTrue(p.getNeuronClass().isInterface(),
                p.getClass().getSimpleName() + " must target an interface");
        assertNotNull(p.getSignalClass());
        assertNotNull(p.getDescription());
        assertFalse(p.hasMerger());
    }

    @Test
    void processors_allInterfaceTyped() {
        assertInterfaceTyped(new MeasurementValidationProcessor());
        assertInterfaceTyped(new MeasurementPIDProcessor());
        assertInterfaceTyped(new MeasurementOscillationProcessor());
        assertInterfaceTyped(new MeasurementInterlockProcessor());
        assertInterfaceTyped(new SetpointPIDProcessor());
        assertInterfaceTyped(new ActuatorSafetyGateProcessor());
        assertInterfaceTyped(new ActuatorDispatchProcessor());
        assertInterfaceTyped(new AlarmAggregationProcessor());
        assertInterfaceTyped(new InterlockModeProcessor());
        assertInterfaceTyped(new DegradationSchedulingProcessor());
        assertInterfaceTyped(new EfficiencyOptimiserProcessor());
        assertInterfaceTyped(new BatchModeProcessor());
        assertInterfaceTyped(new OperatorOverrideProcessor());
        assertInterfaceTyped(new MaintenanceWindowSchedulingProcessor());
    }

    @Test
    void measurementPIDProcessor_emitsActuatorCommand() {
        PIDNeuron pid = new PIDNeuron();
        pid.setTag("act");
        pid.setGains(1.0, 0.0, 0.0);
        pid.setOutputLimits(-10, 10);
        pid.setSetpoint(new SetpointSignal("act", 5.0, 0.0, "ui"));
        MeasurementPIDProcessor p = new MeasurementPIDProcessor();
        p.setDtSeconds(0.1);
        List<ISignal> out = p.process(new MeasurementSignal("act", 0.0, Quality.GOOD, 0L), pid);
        assertEquals(1, out.size());
        assertTrue(out.get(0) instanceof ActuatorCommandSignal);
    }

    @Test
    void safetyGateProcessor_shadowsExecute() {
        SafetyGateNeuron g = new SafetyGateNeuron();
        g.setDefaultMode(SafetyMode.SHADOW);
        ActuatorSafetyGateProcessor p = new ActuatorSafetyGateProcessor();
        List<ISignal> out = p.process(new ActuatorCommandSignal("t", 1.0, 0.0, true), g);
        assertEquals(1, out.size());
        assertFalse(((ActuatorCommandSignal) out.get(0)).isExecute());
    }

    @Test
    void interlockModeProcessor_forcesEmergency() {
        ModeControllerNeuron m = new ModeControllerNeuron();
        new InterlockModeProcessor().process(new InterlockSignal("I", true, null), m);
        assertEquals(PlantMode.EMERGENCY, m.getMode());
    }

    @Test
    void operatorOverrideProcessor_registersOnActuator() {
        ActuatorNeuron a = new ActuatorNeuron();
        new OperatorOverrideProcessor().process(
                new OperatorOverrideSignal("t", OverrideKind.MANUAL, "op", "hold", 4.0), a);
        assertTrue(a.isOverridden("t"));
    }

    @Test
    void batchStateSignal_copyPreservesFields() {
        HashMap<String, Double> km = new HashMap<>();
        km.put("temp", 80.0);
        BatchStateSignal s = new BatchStateSignal("b1", BatchPhase.RUNNING, km);
        BatchStateSignal c = (BatchStateSignal) s.copySignal();
        assertEquals("b1", c.getBatchId());
        assertEquals(BatchPhase.RUNNING, c.getPhase());
        assertEquals(80.0, c.getKeyMetrics().get("temp"), 1e-9);
    }
}
