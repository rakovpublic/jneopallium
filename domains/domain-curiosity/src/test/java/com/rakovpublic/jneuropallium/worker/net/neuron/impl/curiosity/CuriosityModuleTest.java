/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.curiosity;

import com.rakovpublic.jneuropallium.ai.signals.fast.AttentionGateSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.curiosity.BoredomSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.curiosity.EmpowermentSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.curiosity.LearningProgressSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.curiosity.NoveltySignal;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CuriosityModuleTest {

    // ---- NoveltyDetectorNeuron ----

    @Test
    void novelty_firstContextIsFullyNovel() {
        NoveltyDetectorNeuron n = new NoveltyDetectorNeuron();
        NoveltySignal s = n.evaluate("ctx-new");
        assertEquals(1.0, s.getNoveltyScore(), 1e-9);
        assertEquals("ctx-new", s.getContextHash());
    }

    @Test
    void novelty_repeatedContextDecreases() {
        NoveltyDetectorNeuron n = new NoveltyDetectorNeuron();
        double first = n.evaluate("ctx-A").getNoveltyScore();
        double second = n.evaluate("ctx-A").getNoveltyScore();
        assertTrue(second < first, "repeated context should be less novel");
    }

    // ---- LearningProgressNeuron ----

    @Test
    void learningProgress_positiveRewardWhenErrorDecreases() {
        LearningProgressNeuron n = new LearningProgressNeuron();
        n.setWindowTicks(20);
        for (double e : new double[]{1.0, 0.9, 0.8, 0.6, 0.3, 0.1}) {
            n.recordError("nav", e);
        }
        assertTrue(n.intrinsicReward("nav") > 0.0, "decreasing error → positive intrinsic reward");
    }

    @Test
    void learningProgress_negativeOrZeroWhenFlatOrRising() {
        LearningProgressNeuron n = new LearningProgressNeuron();
        for (double e : new double[]{0.1, 0.2, 0.3, 0.4}) {
            n.recordError("manip", e);
        }
        assertTrue(n.intrinsicReward("manip") <= 0.0);
    }

    @Test
    void learningProgress_signalCarriesDomainAndDerivative() {
        LearningProgressNeuron n = new LearningProgressNeuron();
        n.recordError("d1", 1.0);
        LearningProgressSignal s = n.recordError("d1", 0.0);
        assertEquals("d1", s.getDomain());
        assertTrue(s.getErrorDerivative() < 0, "error went down, derivative is negative");
    }

    // ---- EmpowermentNeuron ----

    @Test
    void empowerment_higherForMoreReachableFutures() {
        EmpowermentNeuron n = new EmpowermentNeuron();
        List<List<Integer>> few = Arrays.asList(
                Collections.singletonList(1),
                Collections.singletonList(1));
        List<List<Integer>> many = Arrays.asList(
                Arrays.asList(1, 2, 3),
                Arrays.asList(4, 5, 6));
        EmpowermentSignal low = n.estimate(0, few);
        EmpowermentSignal high = n.estimate(0, many);
        assertTrue(high.getMutualInformation() > low.getMutualInformation());
    }

    @Test
    void empowerment_emptyRolloutsGiveZero() {
        EmpowermentNeuron n = new EmpowermentNeuron();
        assertEquals(0.0, n.estimate(0, null).getMutualInformation(), 1e-9);
        assertEquals(0.0, n.estimate(0, Collections.emptyList()).getMutualInformation(), 1e-9);
    }

    // ---- BoredomNeuron ----

    @Test
    void boredom_familiaritySaturates() {
        BoredomNeuron n = new BoredomNeuron();
        n.setSaturationVisits(4);
        BoredomSignal b1 = n.visit("ctx");
        for (int i = 0; i < 10; i++) n.visit("ctx");
        BoredomSignal bN = n.visit("ctx");
        assertTrue(bN.getFamiliarity() >= b1.getFamiliarity());
        assertEquals(1.0, bN.getFamiliarity(), 1e-9);
    }

    @Test
    void boredom_emitsSuppressionWhenOverThreshold() {
        BoredomNeuron n = new BoredomNeuron();
        n.setSaturationVisits(4);
        n.setFamiliarityThreshold(0.5);
        for (int i = 0; i < 3; i++) n.visit("ctx");
        AttentionGateSignal gate = n.maybeSuppress("ctx");
        assertNotNull(gate);
        assertTrue(gate.isSuppress());
        assertEquals("ctx", gate.getRegionId());
    }

    @Test
    void boredom_noSuppressionBelowThreshold() {
        BoredomNeuron n = new BoredomNeuron();
        n.setSaturationVisits(100);
        n.setFamiliarityThreshold(0.9);
        n.visit("ctx");
        assertNull(n.maybeSuppress("ctx"));
    }

    // ---- Signals ----

    @Test
    void noveltySignal_copyPreservesFields() {
        NoveltySignal s = new NoveltySignal(0.7, "ctx");
        NoveltySignal c = (NoveltySignal) s.copySignal();
        assertEquals(0.7, c.getNoveltyScore(), 1e-9);
        assertEquals("ctx", c.getContextHash());
    }

    @Test
    void noveltySignal_clampsOutOfRange() {
        NoveltySignal s = new NoveltySignal(2.0, "x");
        assertEquals(1.0, s.getNoveltyScore(), 1e-9);
        s.setNoveltyScore(-0.5);
        assertEquals(0.0, s.getNoveltyScore(), 1e-9);
    }

    @Test
    void empowermentSignal_processingFrequency() {
        assertEquals(3L, EmpowermentSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(2, EmpowermentSignal.PROCESSING_FREQUENCY.getLoop());
    }

    @Test
    void boredomSignal_processingFrequency() {
        assertEquals(2L, BoredomSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(2, BoredomSignal.PROCESSING_FREQUENCY.getLoop());
    }

    // ---- Config ----

    @Test
    void curiosityConfig_defaultsAreDisabled() {
        CuriosityConfig cfg = new CuriosityConfig();
        assertFalse(cfg.isEnabled());
        assertEquals(2048, cfg.getNovelty().getHashBits());
        assertEquals(1000, cfg.getNovelty().getDecayTicks());
        assertEquals(200, cfg.getLearningProgress().getWindowTicks());
        assertEquals(3, cfg.getEmpowerment().getHorizon());
        assertEquals(8, cfg.getEmpowerment().getNActionSamples());
        assertEquals(0.2, cfg.getWeights().getBetaNovelty());
        assertEquals(0.1, cfg.getWeights().getBetaEmpowerment());
    }
}
