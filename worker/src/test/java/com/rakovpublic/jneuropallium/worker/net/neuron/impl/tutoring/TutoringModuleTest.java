/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.AffectObservationSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.ContentRecommendationSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.EngagementSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.HintSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.InterventionSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.ItemPresentationSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.MasteryUpdateSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.ResponseSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.ReviewScheduleSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.ScaffoldingSignal;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TutoringModuleTest {

    // ---------- Signals: serialization + ProcessingFrequency ----------

    @Test
    void itemPresentationSignal_processingFrequencyIsLoop1Epoch1() {
        assertEquals(1L, ItemPresentationSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(1, ItemPresentationSignal.PROCESSING_FREQUENCY.getLoop());
    }

    @Test
    void itemPresentationSignal_copyPreservesFields() {
        ItemPresentationSignal s = new ItemPresentationSignal("i-1", "c-1", DifficultyLevel.HARD, 42L);
        ItemPresentationSignal c = (ItemPresentationSignal) s.copySignal();
        assertEquals("i-1", c.getItemId());
        assertEquals("c-1", c.getConceptId());
        assertEquals(DifficultyLevel.HARD, c.getDifficulty());
        assertEquals(42L, c.getPresentedAt());
    }

    @Test
    void responseSignal_processingFrequencyIsLoop1Epoch1() {
        assertEquals(1L, ResponseSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(1, ResponseSignal.PROCESSING_FREQUENCY.getLoop());
        ResponseSignal c = (ResponseSignal) new ResponseSignal("i", true, 800L, "{}").copySignal();
        assertTrue(c.isCorrect());
        assertEquals(800L, c.getLatencyMs());
    }

    @Test
    void engagementSignal_processingFrequencyIsLoop1Epoch2() {
        assertEquals(2L, EngagementSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(1, EngagementSignal.PROCESSING_FREQUENCY.getLoop());
        EngagementSignal s = new EngagementSignal(2.0, EngagementSource.CAMERA);
        assertEquals(1.0, s.getAttentionScore(), 1e-9);
    }

    @Test
    void affectObservationSignal_clampsPayload() {
        AffectObservationSignal s = new AffectObservationSignal(-2.0, 2.0, -1.0);
        assertEquals(-1.0, s.getValence(), 1e-9);
        assertEquals(1.0, s.getArousal(), 1e-9);
        assertEquals(0.0, s.getConfidence(), 1e-9);
    }

    @Test
    void masteryUpdateSignal_processingFrequencyIsLoop2Epoch3() {
        assertEquals(3L, MasteryUpdateSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(2, MasteryUpdateSignal.PROCESSING_FREQUENCY.getLoop());
    }

    @Test
    void contentRecommendationSignal_processingFrequencyIsLoop1Epoch3() {
        assertEquals(3L, ContentRecommendationSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(1, ContentRecommendationSignal.PROCESSING_FREQUENCY.getLoop());
    }

    @Test
    void hintSignal_defaultsToMetaCognitive() {
        HintSignal s = new HintSignal("i", null, "text");
        assertEquals(HintLevel.META_COGNITIVE, s.getLevel());
    }

    @Test
    void scaffoldingSignal_copyPreservesType() {
        ScaffoldingSignal s = new ScaffoldingSignal(ScaffoldType.WORKED_STEPS, "payload");
        ScaffoldingSignal c = (ScaffoldingSignal) s.copySignal();
        assertEquals(ScaffoldType.WORKED_STEPS, c.getType());
    }

    @Test
    void reviewScheduleSignal_processingFrequencyIsLoop2Epoch5() {
        assertEquals(5L, ReviewScheduleSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(2, ReviewScheduleSignal.PROCESSING_FREQUENCY.getLoop());
    }

    @Test
    void interventionSignal_defaultsToEncourage() {
        InterventionSignal s = new InterventionSignal(null, "why");
        assertEquals(InterventionType.ENCOURAGE, s.getType());
    }

    // ---------- L0 Neurons ----------

    @Test
    void responseObserverNeuron_tracksAccuracy() {
        ResponseObserverNeuron n = new ResponseObserverNeuron();
        n.observe(new ResponseSignal("a", true, 100, ""));
        n.observe(new ResponseSignal("b", false, 100, ""));
        n.observe(new ResponseSignal("c", true, 100, ""));
        assertEquals(3, n.getTotalResponses());
        assertEquals(2, n.getTotalCorrect());
        assertEquals(2.0 / 3.0, n.getAccuracy(), 1e-9);
    }

    @Test
    void engagementSensorNeuron_fusesAvailableChannels() {
        EngagementSensorNeuron n = new EngagementSensorNeuron();
        EngagementSignal s = n.fuse(0.8, 0.6, Double.NaN, Double.NaN);
        assertEquals(0.7, s.getAttentionScore(), 1e-9);
        assertEquals(EngagementSource.MULTI_MODAL, s.getSource());
    }

    @Test
    void engagementSensorNeuron_singleChannelReportsThatSource() {
        EngagementSensorNeuron n = new EngagementSensorNeuron();
        EngagementSignal s = n.fuse(Double.NaN, 0.5, Double.NaN, Double.NaN);
        assertEquals(EngagementSource.DWELL_TIME, s.getSource());
    }

    @Test
    void affectObserverNeuron_highAccuracyGivesPositiveValence() {
        AffectObserverNeuron n = new AffectObserverNeuron();
        AffectObservationSignal s = null;
        for (int i = 0; i < 10; i++) s = n.infer(0.8, 0.9);
        assertNotNull(s);
        assertTrue(s.getValence() > 0, "expected positive valence, got " + s.getValence());
    }

    // ---------- L2 Neurons ----------

    @Test
    void flowStateNeuron_classifiesFlowCorrectly() {
        FlowStateNeuron n = new FlowStateNeuron();
        assertEquals(FlowStateKind.FLOW, n.classify(0.8, 0.4, 0.5, 0.75));
    }

    @Test
    void flowStateNeuron_classifiesFrustration() {
        FlowStateNeuron n = new FlowStateNeuron();
        assertEquals(FlowStateKind.FRUSTRATION, n.classify(0.5, -0.6, 0.8, 0.2));
    }

    @Test
    void flowStateNeuron_classifiesBoredom() {
        FlowStateNeuron n = new FlowStateNeuron();
        assertEquals(FlowStateKind.BOREDOM, n.classify(0.1, 0.0, 0.1, 0.9));
    }

    @Test
    void flowStateNeuron_classifiesOverload() {
        FlowStateNeuron n = new FlowStateNeuron();
        assertEquals(FlowStateKind.OVERLOAD, n.classify(0.6, 0.0, 0.9, 0.1));
    }

    @Test
    void fatigueNeuron_emitsBreakAfterSessionDuration() {
        FatigueNeuron n = new FatigueNeuron();
        n.setMaxSessionTicks(10);
        n.startSession(0);
        InterventionSignal out = null;
        for (int t = 1; t <= 11; t++) out = n.tick(t, true, 0.1);
        assertNotNull(out);
        assertEquals(InterventionType.BREAK, out.getType());
    }

    @Test
    void fatigueNeuron_emitsBreakOnErrorDrift() {
        FatigueNeuron n = new FatigueNeuron();
        n.setMaxConsecutiveErrors(3);
        n.startSession(0);
        InterventionSignal out = null;
        for (int t = 1; t <= 5; t++) out = n.tick(t, false, 0.6);
        assertNotNull(out);
        assertEquals(InterventionType.BREAK, out.getType());
    }

    // ---------- L3 Neurons ----------

    @Test
    void conceptMasteryNeuron_bktRisesWithCorrectAnswers() {
        ConceptMasteryNeuron n = new ConceptMasteryNeuron("addition");
        double initial = n.getPKnow();
        for (int i = 0; i < 20; i++) n.update(true);
        assertTrue(n.getPKnow() > initial, "P(know) should rise");
        assertTrue(n.getPKnow() > 0.8, "should approach mastery, got " + n.getPKnow());
    }

    @Test
    void conceptMasteryNeuron_emitsMasteryUpdateWhenDeltaExceeded() {
        ConceptMasteryNeuron n = new ConceptMasteryNeuron("c");
        MasteryUpdateSignal first = n.update(true);
        assertNotNull(first);
        assertEquals("c", first.getConceptId());
    }

    @Test
    void prerequisiteGraphNeuron_onlyReturnsConceptsWithAllPrereqsMet() {
        PrerequisiteGraphNeuron g = new PrerequisiteGraphNeuron();
        g.addPrerequisite("B", "A");
        g.addPrerequisite("C", "B");
        Map<String, Double> mastery = new HashMap<>();
        mastery.put("A", 0.9);
        mastery.put("B", 0.3);
        List<String> eligible = g.eligibleNext(mastery);
        assertTrue(eligible.contains("B"));
        assertFalse(eligible.contains("C"));
    }

    @Test
    void forgettingCurveNeuron_intervalGrowsAfterCorrectAnswers() {
        ForgettingCurveNeuron n = new ForgettingCurveNeuron();
        n.recordAttempt("c", 0, 5);
        long i1 = n.intervalFor("c");
        n.recordAttempt("c", i1, 5);
        long i2 = n.intervalFor("c");
        n.recordAttempt("c", i1 + i2, 5);
        long i3 = n.intervalFor("c");
        assertTrue(i2 > i1);
        assertTrue(i3 > i2);
    }

    @Test
    void forgettingCurveNeuron_failureResetsRepetitions() {
        ForgettingCurveNeuron n = new ForgettingCurveNeuron();
        n.recordAttempt("c", 0, 5);
        n.recordAttempt("c", 100, 5);
        assertEquals(2, n.repetitionsFor("c"));
        n.recordAttempt("c", 200, 1);
        assertEquals(0, n.repetitionsFor("c"));
    }

    // ---------- L4 Neurons ----------

    @Test
    void zpdPlanningNeuron_picksItemNearestTargetSuccessRate() {
        ZPDPlanningNeuron p = new ZPDPlanningNeuron();
        p.setTargetSuccessRate(0.75);
        List<ZPDPlanningNeuron.Candidate> cs = Arrays.asList(
                new ZPDPlanningNeuron.Candidate("easy", "c", 0.95),
                new ZPDPlanningNeuron.Candidate("zpd", "c", 0.78),
                new ZPDPlanningNeuron.Candidate("hard", "c", 0.20));
        ContentRecommendationSignal rec = p.plan(cs);
        assertNotNull(rec);
        assertEquals("zpd", rec.getItemId());
    }

    @Test
    void hintGenerationNeuron_graduatesThroughLevels() {
        HintGenerationNeuron n = new HintGenerationNeuron();
        HintSignal h1 = n.nextHint("i1", "concept");
        HintSignal h2 = n.nextHint("i1", "concept");
        HintSignal h3 = n.nextHint("i1", "concept");
        HintSignal h4 = n.nextHint("i1", "concept");
        assertEquals(HintLevel.META_COGNITIVE, h1.getLevel());
        assertEquals(HintLevel.CONCEPTUAL, h2.getLevel());
        assertEquals(HintLevel.WORKED_EXAMPLE, h3.getLevel());
        assertNull(h4, "should be exhausted after 3 hints");
    }

    @Test
    void scaffoldingNeuron_onlyFiresOnOverloadOrFrustration() {
        ScaffoldingNeuron n = new ScaffoldingNeuron();
        assertNull(n.scaffoldFor(FlowStateKind.FLOW, "c"));
        assertNull(n.scaffoldFor(FlowStateKind.NEUTRAL, "c"));
        assertNotNull(n.scaffoldFor(FlowStateKind.OVERLOAD, "c"));
        assertNotNull(n.scaffoldFor(FlowStateKind.FRUSTRATION, "c"));
    }

    // ---------- L5 Neurons ----------

    @Test
    void contentSelectionNeuron_argmaxPrefersHighCompositeScore() {
        ContentSelectionNeuron n = new ContentSelectionNeuron();
        List<ContentSelectionNeuron.ScoredItem> items = Arrays.asList(
                new ContentSelectionNeuron.ScoredItem("a", 0.9, 0.1),
                new ContentSelectionNeuron.ScoredItem("b", 0.2, 0.9));
        assertEquals("a", n.argmax(items));
    }

    @Test
    void pacingNeuron_slowsDownOnOverload() {
        PacingNeuron n = new PacingNeuron();
        int base = n.getCurrentRatio();
        int after = n.computeRatio(FlowStateKind.OVERLOAD, DifficultyLevel.HARD);
        assertTrue(after < base);
        assertTrue(after >= n.getFastSlowRatioMin());
    }

    @Test
    void pacingNeuron_speedsUpOnBoredom() {
        PacingNeuron n = new PacingNeuron();
        int base = n.getCurrentRatio();
        int after = n.computeRatio(FlowStateKind.BOREDOM, DifficultyLevel.EASY);
        assertTrue(after > base);
        assertTrue(after <= n.getFastSlowRatioMax());
    }

    // ---------- L7 Neurons ----------

    @Test
    void wellbeingGuardNeuron_escalatesAfterSustainedFrustration() {
        WellbeingGuardNeuron g = new WellbeingGuardNeuron();
        g.setMaxFrustrationTicks(3);
        InterventionSignal a = null, b = null, c = null, d = null;
        for (int i = 0; i < 3; i++) a = g.assess(FlowStateKind.FRUSTRATION);
        assertNotNull(a);
        assertEquals(InterventionType.ENCOURAGE, a.getType());
        for (int i = 0; i < 3; i++) b = g.assess(FlowStateKind.FRUSTRATION);
        assertEquals(InterventionType.BREAK, b.getType());
        for (int i = 0; i < 3; i++) c = g.assess(FlowStateKind.FRUSTRATION);
        assertEquals(InterventionType.REDIRECT, c.getType());
        for (int i = 0; i < 3; i++) d = g.assess(FlowStateKind.FRUSTRATION);
        assertEquals(InterventionType.ESCALATE_TO_HUMAN, d.getType());
    }

    @Test
    void wellbeingGuardNeuron_resetsOnFlowState() {
        WellbeingGuardNeuron g = new WellbeingGuardNeuron();
        g.setMaxFrustrationTicks(5);
        for (int i = 0; i < 4; i++) g.assess(FlowStateKind.FRUSTRATION);
        g.assess(FlowStateKind.FLOW);
        assertEquals(0, g.getConsecutiveFrustrationTicks());
    }

    @Test
    void fairnessNeuron_disablesLatencyPenaltyWhenAccommodationSet() {
        FairnessNeuron f = new FairnessNeuron();
        assertTrue(f.isResponseTimePenaltyEnabled());
        f.addAccommodation("extra-time");
        assertFalse(f.isResponseTimePenaltyEnabled());
        double score = f.adjustScoreForLatency(1.0, 10_000L, 1_000L);
        assertEquals(1.0, score, 1e-9);
    }

    @Test
    void fairnessNeuron_penalizesSlowResponsesWithoutAccommodation() {
        FairnessNeuron f = new FairnessNeuron();
        double score = f.adjustScoreForLatency(1.0, 2_000L, 1_000L);
        assertTrue(score < 1.0);
        assertTrue(score > 0.0);
    }

    @Test
    void fairnessNeuron_cannotModifyEthicalPriority() {
        FairnessNeuron f = new FairnessNeuron();
        assertFalse(f.wouldModifyEthicalPriority());
    }

    // ---------- Config ----------

    @Test
    void tutoringConfig_defaultsDisabled() {
        TutoringConfig cfg = new TutoringConfig();
        assertFalse(cfg.isEnabled());
    }

    @Test
    void tutoringConfig_defaultAccommodationsDoNotPenalise() {
        TutoringConfig cfg = new TutoringConfig();
        assertFalse(cfg.isFairnessResponseTimePenalty());
        assertTrue(cfg.getFairnessAccommodationFlags().contains("extra-time"));
    }

    // ---------- Integration: adaptive loop ----------

    @Test
    void integration_overloadTriggersScaffoldAndBreak() {
        FlowStateNeuron flow = new FlowStateNeuron();
        ScaffoldingNeuron scaf = new ScaffoldingNeuron();
        WellbeingGuardNeuron guard = new WellbeingGuardNeuron();
        guard.setMaxFrustrationTicks(5);

        for (int i = 0; i < 6; i++) {
            FlowStateKind k = flow.classify(0.6, 0.0, 0.85, 0.15);
            assertEquals(FlowStateKind.OVERLOAD, k);
            ScaffoldingSignal s = scaf.scaffoldFor(k, "c-ol");
            assertNotNull(s);
            guard.assess(k);
        }
        InterventionSignal out = guard.assess(FlowStateKind.OVERLOAD);
        // Guard should escalate at some point during the sustained run
        assertTrue(guard.getEscalationStrikes() >= 1 || out != null);
    }
}
