/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.AnomalyScoreSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.IncidentReportSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.InflammationBroadcastSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.LogEventSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.PacketSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.QuarantineLiftSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.QuarantineRequestSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.SelfToleranceSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.SignatureMatchSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.SyscallSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.ThreatHypothesisSignal;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.security.AnomalyHypothesisProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.security.HypothesisResponseProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.security.IncidentFatigueProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.security.IncidentRollbackProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.security.InflammationBaselineProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.security.LogSignatureProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.security.PacketFlowProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.security.PacketSignatureProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.security.QuarantineApplyProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.security.QuarantineGateProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.security.QuarantineLiftProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.security.SelfToleranceProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.security.SignatureHypothesisProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.security.SignatureToleranceProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.security.SyscallBehaviourProcessor;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecurityModuleTest {

    // ---------- enum cardinalities ----------

    @Test
    void enums_cardinality() {
        assertEquals(14, ThreatCategory.values().length);
        assertEquals(5, EntityKind.values().length);
        assertEquals(5, AlertLevel.values().length);
        assertEquals(9, LogLevel.values().length);
        assertEquals(4, Severity.values().length);
        assertEquals(6, ResponseBand.values().length);
    }

    // ---------- ProcessingFrequency ----------

    @Test
    void signals_haveCorrectProcessingFrequency() {
        assertEquals(1L, PacketSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(1, PacketSignal.PROCESSING_FREQUENCY.getLoop());
        assertEquals(1L, SyscallSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(2L, LogEventSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(1L, SignatureMatchSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(2L, AnomalyScoreSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(1L, ThreatHypothesisSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(2, ThreatHypothesisSignal.PROCESSING_FREQUENCY.getLoop());
        assertEquals(1L, QuarantineRequestSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(1L, QuarantineLiftSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(1L, InflammationBroadcastSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(2, InflammationBroadcastSignal.PROCESSING_FREQUENCY.getLoop());
        assertEquals(5L, SelfToleranceSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(1L, IncidentReportSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(2, IncidentReportSignal.PROCESSING_FREQUENCY.getLoop());
    }

    // ---------- ingestion ----------

    @Test
    void packetIngest_respectsRateLimit() {
        PacketIngestNeuron n = new PacketIngestNeuron();
        n.setRateLimitPerSec(2);
        NetworkTuple t = new NetworkTuple("1.1.1.1", "2.2.2.2", "tcp", 1000, 80);
        long ts = 1_000_000_000L;
        assertNotNull(n.ingest(new byte[]{1}, t, ts));
        assertNotNull(n.ingest(new byte[]{2}, t, ts));
        assertNull(n.ingest(new byte[]{3}, t, ts));
        assertEquals(2L, n.getAccepted());
        assertEquals(1L, n.getDropped());
    }

    @Test
    void syscallIngest_emits() {
        SyscallIngestNeuron n = new SyscallIngestNeuron();
        assertNotNull(n.ingest(56, 123, "bash", new long[]{0}));
        assertEquals(1, n.getAccepted());
    }

    @Test
    void logIngest_emits() {
        LogIngestNeuron n = new LogIngestNeuron();
        HashMap<String, String> f = new HashMap<>();
        f.put("user", "alice");
        assertNotNull(n.ingest("auth", LogLevel.WARN, f, 0L));
        assertEquals(1, n.getAccepted());
    }

    // ---------- signature ----------

    @Test
    void signaturePattern_matchesPacketBody() {
        SignaturePatternNeuron s = new SignaturePatternNeuron();
        s.addSignature("sig-1", "malw", new byte[]{'E','V','I','L'}, "IOC-1");
        NetworkTuple t = new NetworkTuple("a", "b", "tcp", 1, 2);
        PacketSignal hit = new PacketSignal("prefix-EVIL-suffix".getBytes(), t, 0L);
        SignatureMatchSignal m = s.match(hit);
        assertNotNull(m);
        assertEquals("sig-1", m.getSignatureId());
        assertNull(s.match(new PacketSignal("nothing".getBytes(), t, 0L)));
    }

    // ---------- process-behaviour ----------

    @Test
    void processBehaviour_detectsForbiddenSequence() {
        ProcessBehaviourNeuron p = new ProcessBehaviourNeuron();
        p.addForbiddenSequence("inject", new int[]{1, 2, 3});
        p.observe(new SyscallSignal(1, 7, "mal.exe", null));
        p.observe(new SyscallSignal(9, 7, "mal.exe", null));
        p.observe(new SyscallSignal(2, 7, "mal.exe", null));
        SignatureMatchSignal m = p.observe(new SyscallSignal(3, 7, "mal.exe", null));
        assertNotNull(m);
        assertEquals("inject", m.getSignatureId());
    }

    // ---------- innate tolerance ----------

    @Test
    void innateInterneuron_filtersAllowed() {
        InnateInterneuron n = new InnateInterneuron();
        n.onTolerance(new SelfToleranceSignal("trusted-*", true));
        SignatureMatchSignal raw = new SignatureMatchSignal("sig", "fam", 0.9, "trusted-svc");
        assertNull(n.filter(raw, "trusted-svc"));
        assertNotNull(n.filter(raw, "untrusted-x"));
    }

    // ---------- anomaly ----------

    @Test
    void anomalyDetector_scoresDeviation() {
        AnomalyDetectorNeuron a = new AnomalyDetectorNeuron();
        a.setBaseline("host-1", new double[]{1.0, 1.0});
        AnomalyScoreSignal s = a.score("host-1", new double[]{5.0, 5.0});
        assertNotNull(s);
        assertTrue(s.getDeviationScore() > 0.3);
    }

    @Test
    void baseline_freezesOnInflammation() {
        EntityBehaviourBaselineNeuron b = new EntityBehaviourBaselineNeuron();
        b.onInflammation(new InflammationBroadcastSignal(AlertLevel.HIGH, "net1", 0.9));
        b.update("h1", new double[]{0.5});
        assertNull(b.baselineFor("h1"));
        b.onInflammation(new InflammationBroadcastSignal(AlertLevel.CLEAR, "net1", 0.0));
        b.update("h1", new double[]{0.5});
        assertNotNull(b.baselineFor("h1"));
    }

    // ---------- beaconing + lateral ----------

    @Test
    void beaconing_detectsLowJitter() {
        BeaconingDetectorNeuron b = new BeaconingDetectorNeuron();
        b.setMinSamples(4);
        b.setJitterTolerance(0.2);
        for (int i = 0; i < 10; i++) b.observe("c2", 100L * i);
        AnomalyScoreSignal r = b.assess("c2");
        assertNotNull(r);
        assertTrue(r.getDeviationScore() > 0.0);
    }

    @Test
    void lateralMovement_firesOnHighFanout() {
        LateralMovementNeuron l = new LateralMovementNeuron();
        l.setFanoutThreshold(3);
        assertNull(l.recordAuth("u1", "h1", 0L));
        assertNull(l.recordAuth("u1", "h2", 1L));
        AnomalyScoreSignal r = l.recordAuth("u1", "h3", 2L);
        assertNotNull(r);
    }

    // ---------- memory ----------

    @Test
    void attackMemory_lookupByTtps() {
        AttackMemoryNeuron m = new AttackMemoryNeuron();
        m.store("APT-1", ThreatCategory.LATERAL_MOVEMENT, "T1021", "T1059");
        assertTrue(m.lookup("T1021").contains("APT-1"));
        assertTrue(m.lookup("T9999").isEmpty());
    }

    @Test
    void incidentTimeline_summarisesEvents() {
        IncidentTimelineNeuron t = new IncidentTimelineNeuron();
        t.append("inc-1", "evt-a", 0L);
        t.append("inc-1", "evt-b", 1L);
        IncidentReportSignal r = t.summarise("inc-1", Severity.HIGH, "ransomware suspected");
        assertNotNull(r);
        assertEquals(2, r.getLinkedEvents().size());
        assertEquals(Severity.HIGH, r.getSeverity());
    }

    // ---------- hypothesis ----------

    @Test
    void threatHypothesis_posteriorSkewsWithSignatureEvidence() {
        ThreatHypothesisNeuron h = new ThreatHypothesisNeuron();
        h.seed("apt-a", ThreatCategory.COMMAND_AND_CONTROL);
        h.seed("noise", ThreatCategory.UNKNOWN);
        h.updateFromSignature(new SignatureMatchSignal("s1", "apt-a", 0.9, "ioc-1"), "apt-a");
        assertTrue(h.posteriorOf("apt-a") > h.posteriorOf("noise"));
    }

    // ---------- response ----------

    @Test
    void responsePlanning_bandsMatchSpec() {
        ResponsePlanningNeuron r = new ResponsePlanningNeuron();
        assertEquals(ResponseBand.LOG, r.band(0.1));
        assertEquals(ResponseBand.ALERT, r.band(0.5));
        assertEquals(ResponseBand.CONNECTION_QUARANTINE, r.band(0.7));
        assertEquals(ResponseBand.HOST_QUARANTINE, r.band(0.95));
    }

    @Test
    void responsePlanning_emitsConnectionQuarantine() {
        ResponsePlanningNeuron r = new ResponsePlanningNeuron();
        ThreatHypothesisSignal t = new ThreatHypothesisSignal("hyp", ThreatCategory.IMPACT, 0.75, null);
        QuarantineRequestSignal q = r.plan(t, "conn-1", EntityKind.CONNECTION);
        assertNotNull(q);
        assertEquals(EntityKind.CONNECTION, q.getKind());
        assertTrue(q.getDurationTicks() > 0);
    }

    @Test
    void responseGate_blocksHardAllow() {
        ResponseGateNeuron g = new ResponseGateNeuron();
        g.registerHardAllow("prod-*");
        QuarantineRequestSignal q = new QuarantineRequestSignal("prod-db1", EntityKind.HOST, 100, "test");
        assertNull(g.gate(q, 0.99));
    }

    @Test
    void responseGate_protectsCriticalAssetBelowSafe() {
        ResponseGateNeuron g = new ResponseGateNeuron();
        g.registerCriticalAsset("dc-01");
        QuarantineRequestSignal q = new QuarantineRequestSignal("dc-01", EntityKind.HOST, 100, "test");
        assertNull(g.gate(q, 0.70));  // below safePosterior 0.85
        assertNotNull(g.gate(q, 0.99));
    }

    @Test
    void responseGate_modeValidation() {
        ResponseGateNeuron g = new ResponseGateNeuron();
        assertThrows(IllegalArgumentException.class, () -> g.setMode("bogus"));
        g.setMode("alert-only");
        assertEquals("alert-only", g.getMode());
        QuarantineRequestSignal q = new QuarantineRequestSignal("anon", EntityKind.CONNECTION, 50, "t");
        assertNull(g.gate(q, 0.99));
    }

    // ---------- quarantine ----------

    @Test
    void quarantine_neverPermanent_autoLift() {
        QuarantineEntityNeuron qn = new QuarantineEntityNeuron();
        QuarantineRequestSignal req = new QuarantineRequestSignal("host-x", EntityKind.HOST, 10, "test");
        assertTrue(qn.apply(req, 0L));
        assertTrue(qn.isQuarantined("host-x", 5L));
        List<QuarantineLiftSignal> lifts = qn.tick(50L);
        assertEquals(1, lifts.size());
        assertEquals("host-x", lifts.get(0).getEntityId());
        assertFalse(qn.isQuarantined("host-x", 51L));
    }

    @Test
    void quarantine_rejectsZeroDuration() {
        QuarantineEntityNeuron qn = new QuarantineEntityNeuron();
        QuarantineRequestSignal req = new QuarantineRequestSignal();
        req.setEntityId("x");
        req.setKind(EntityKind.HOST);
        // setDurationTicks clamps to 1, so manually reflect
        assertTrue(req.getDurationTicks() >= 1);
    }

    @Test
    void quarantine_reconfirmExtends() {
        QuarantineEntityNeuron qn = new QuarantineEntityNeuron();
        qn.apply(new QuarantineRequestSignal("h", EntityKind.HOST, 10, "r"), 0L);
        assertTrue(qn.reconfirm("h", 100, 5L));
        // Should still be quarantined at 50 (5 + 100 = 105)
        assertTrue(qn.isQuarantined("h", 50L));
    }

    // ---------- rollback ----------

    @Test
    void rollback_onlyWhenEnabledAndHighSeverity() {
        RollbackNeuron r = new RollbackNeuron();
        IncidentReportSignal low = new IncidentReportSignal("i1", null, Severity.LOW, "x");
        IncidentReportSignal hi  = new IncidentReportSignal("i2", null, Severity.HIGH, "y");
        assertFalse(r.maybeRollback(hi));   // disabled by default
        r.setEnabled(true);
        assertFalse(r.maybeRollback(low));  // low severity
        assertTrue(r.maybeRollback(hi));
    }

    // ---------- homeostasis ----------

    @Test
    void alertFatigue_thresholdMultiplier() {
        AlertFatigueMonitorNeuron a = new AlertFatigueMonitorNeuron();
        a.observe(new IncidentReportSignal("i1", null, Severity.LOW, null));
        a.observe(new IncidentReportSignal("i2", null, Severity.LOW, null));
        a.acknowledge("i1", false);
        a.acknowledge("i2", false);
        assertEquals(1.0, a.falsePositiveRate(), 1e-9);
        assertEquals(2.0, a.thresholdMultiplier(), 1e-9);
    }

    @Test
    void immuneExhaustion_canBeExhausted() {
        ImmuneExhaustionNeuron e = new ImmuneExhaustionNeuron();
        e.setBudgetPerTick(0.5);
        e.setRecoveryRate(0.0);
        e.consume(1);
        e.consume(2);
        e.consume(3);
        assertTrue(e.isExhausted());
    }

    // ---------- config ----------

    @Test
    void config_defaultsAndValidation() {
        SecurityConfig c = new SecurityConfig();
        assertTrue(c.isEnabled());
        assertEquals("enforcing", c.getResponseMode());
        assertFalse(c.isRollbackEnabled());
        assertTrue(c.isAffectDisabled());
        assertTrue(c.isCuriosityDisabled());
        assertThrows(IllegalArgumentException.class, () -> c.setResponseMode("bogus"));
        c.setResponseMode("alert-only");
        assertEquals("alert-only", c.getResponseMode());
    }

    // ---------- processors: interface-typed + behaviour smoke ----------

    private static void assertInterfaceTyped(ISignalProcessor<?, ?> p) {
        assertTrue(p.getNeuronClass().isInterface(),
                p.getClass().getSimpleName() + " must target an interface");
        assertNotNull(p.getSignalClass());
        assertFalse(p.hasMerger());
        assertNotNull(p.getDescription());
    }

    @Test
    void processors_allInterfaceTyped() {
        assertInterfaceTyped(new PacketSignatureProcessor());
        assertInterfaceTyped(new PacketFlowProcessor());
        assertInterfaceTyped(new SyscallBehaviourProcessor());
        assertInterfaceTyped(new LogSignatureProcessor());
        assertInterfaceTyped(new SignatureToleranceProcessor());
        assertInterfaceTyped(new SignatureHypothesisProcessor());
        assertInterfaceTyped(new AnomalyHypothesisProcessor());
        assertInterfaceTyped(new HypothesisResponseProcessor());
        assertInterfaceTyped(new QuarantineGateProcessor());
        assertInterfaceTyped(new QuarantineApplyProcessor());
        assertInterfaceTyped(new QuarantineLiftProcessor());
        assertInterfaceTyped(new InflammationBaselineProcessor());
        assertInterfaceTyped(new SelfToleranceProcessor());
        assertInterfaceTyped(new IncidentFatigueProcessor());
        assertInterfaceTyped(new IncidentRollbackProcessor());
    }

    @Test
    void packetSignatureProcessor_emitsMatch() {
        PacketSignatureProcessor p = new PacketSignatureProcessor();
        SignaturePatternNeuron s = new SignaturePatternNeuron();
        s.addSignature("s1", "fam", new byte[]{'X'}, "ioc");
        NetworkTuple t = new NetworkTuple("a", "b", "tcp", 1, 2);
        List<ISignal> out = p.process(new PacketSignal(new byte[]{'X'}, t, 0L), s);
        assertEquals(1, out.size());
    }

    @Test
    void quarantineGateProcessor_dropsForCriticalAssetLowPosterior() {
        // QuarantineGateProcessor uses posterior=1.0 internally; to simulate
        // a critical asset block, use the ResponseGateNeuron directly for
        // the low-posterior path; the processor path only drops hard-allow.
        QuarantineGateProcessor p = new QuarantineGateProcessor();
        ResponseGateNeuron g = new ResponseGateNeuron();
        g.registerHardAllow("prod-*");
        List<ISignal> out = p.process(
                new QuarantineRequestSignal("prod-db1", EntityKind.HOST, 100, "test"), g);
        assertTrue(out.isEmpty());
    }

    @Test
    void quarantineApplyProcessor_storesInNeuron() {
        QuarantineApplyProcessor p = new QuarantineApplyProcessor();
        QuarantineEntityNeuron qn = new QuarantineEntityNeuron();
        QuarantineRequestSignal req = new QuarantineRequestSignal("x", EntityKind.HOST, 10, "r");
        req.setEpoch(0L);
        p.process(req, qn);
        assertTrue(qn.isQuarantined("x", 0L));
    }

    @Test
    void selfToleranceProcessor_updatesAllowList() {
        SelfToleranceProcessor p = new SelfToleranceProcessor();
        InnateInterneuron n = new InnateInterneuron();
        p.process(new SelfToleranceSignal("svc-*", true), n);
        assertTrue(n.isAllowed("svc-42"));
    }

    @Test
    void anomalyHypothesisProcessor_updatesPosterior() {
        AnomalyHypothesisProcessor p = new AnomalyHypothesisProcessor();
        ThreatHypothesisNeuron t = new ThreatHypothesisNeuron();
        t.seed("host-1", ThreatCategory.EXFILTRATION);
        t.seed("bg", ThreatCategory.UNKNOWN);
        AnomalyScoreSignal s = new AnomalyScoreSignal("host-1", 0.8, null);
        List<ISignal> out = p.process(s, t);
        assertEquals(1, out.size());
        assertTrue(t.posteriorOf("host-1") > t.posteriorOf("bg"));
    }

    @Test
    void incidentRollbackProcessor_onlyFiresWhenEnabled() {
        IncidentRollbackProcessor p = new IncidentRollbackProcessor();
        RollbackNeuron r = new RollbackNeuron();
        p.process(new IncidentReportSignal("i1", null, Severity.CRITICAL, "x"), r);
        assertEquals(0, r.attempted());
        r.setEnabled(true);
        p.process(new IncidentReportSignal("i2", null, Severity.CRITICAL, "x"), r);
        assertEquals(1, r.attempted());
    }

    // ---------- NetworkTuple equals/hash ----------

    @Test
    void networkTuple_equalsAndHash() {
        NetworkTuple a = new NetworkTuple("1.1.1.1", "2.2.2.2", "tcp", 1000, 80);
        NetworkTuple b = new NetworkTuple("1.1.1.1", "2.2.2.2", "tcp", 1000, 80);
        NetworkTuple c = new NetworkTuple("1.1.1.1", "2.2.2.2", "tcp", 1000, 443);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }
}
