/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.sleep;

import com.rakovpublic.jneuropallium.worker.net.signals.impl.sleep.DreamSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.sleep.ReplaySignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.sleep.SharpWaveRippleSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.sleep.SleepStateSignal;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SleepModuleTest {

    // ---- SleepControllerNeuron ----

    @Test
    void sleepPhaseCycle_visitsAllPhasesInOrder() {
        SleepControllerNeuron ctl = new SleepControllerNeuron();
        ctl.setCycleTicks(20);
        ctl.setNremFraction(0.5);
        ctl.setRemFraction(0.25);

        EnumSet<SleepPhase> seen = EnumSet.noneOf(SleepPhase.class);
        int firstWake = -1, firstNrem2 = -1, firstNrem3 = -1, firstRem = -1;
        for (int i = 0; i < 19; i++) {
            SleepStateSignal s = ctl.advance();
            seen.add(s.getPhase());
            if (firstWake < 0 && s.getPhase() == SleepPhase.WAKE) firstWake = i;
            if (firstNrem2 < 0 && s.getPhase() == SleepPhase.NREM2) firstNrem2 = i;
            if (firstNrem3 < 0 && s.getPhase() == SleepPhase.NREM3) firstNrem3 = i;
            if (firstRem < 0 && s.getPhase() == SleepPhase.REM) firstRem = i;
        }
        assertTrue(seen.contains(SleepPhase.WAKE));
        assertTrue(seen.contains(SleepPhase.NREM2));
        assertTrue(seen.contains(SleepPhase.NREM3));
        assertTrue(seen.contains(SleepPhase.REM));
        assertTrue(firstWake < firstNrem2 && firstNrem2 < firstNrem3 && firstNrem3 < firstRem,
                "within one cycle the order is WAKE → NREM2 → NREM3 → REM");
    }

    @Test
    void sleepController_nrem3DeeperThanNrem2() {
        SleepControllerNeuron ctl = new SleepControllerNeuron();
        ctl.setCycleTicks(100);
        ctl.setNremFraction(0.6);
        ctl.setRemFraction(0.15);
        double maxN2 = 0, maxN3 = 0;
        for (int i = 0; i < 100; i++) {
            SleepStateSignal s = ctl.advance();
            if (s.getPhase() == SleepPhase.NREM2) maxN2 = Math.max(maxN2, s.getDepth());
            if (s.getPhase() == SleepPhase.NREM3) maxN3 = Math.max(maxN3, s.getDepth());
        }
        assertTrue(maxN3 >= maxN2);
    }

    // ---- HippocampalReplayNeuron ----

    @Test
    void replay_reverseInvertsSequence() {
        HippocampalReplayNeuron rep = new HippocampalReplayNeuron();
        rep.setDirection(ReplayDirection.REVERSE);
        rep.setCompressionRatio(8.0);
        rep.recordEpisode("ep1", Arrays.asList(1L, 2L, 3L, 4L), 0.9);
        List<ReplaySignal> out = rep.emitTopK();
        assertEquals(1, out.size());
        assertEquals(Arrays.asList(4L, 3L, 2L, 1L), out.get(0).getNeuronSequence());
        assertEquals(ReplayDirection.REVERSE, out.get(0).getDirection());
        assertEquals(8.0, out.get(0).getCompressionRatio());
    }

    @Test
    void replay_forwardPreservesOrder() {
        HippocampalReplayNeuron rep = new HippocampalReplayNeuron();
        rep.setDirection(ReplayDirection.FORWARD);
        rep.recordEpisode("ep", Arrays.asList(1L, 2L, 3L), 0.5);
        List<ReplaySignal> out = rep.emitTopK();
        assertEquals(Arrays.asList(1L, 2L, 3L), out.get(0).getNeuronSequence());
    }

    @Test
    void replay_topKSalienceOrdering() {
        HippocampalReplayNeuron rep = new HippocampalReplayNeuron();
        rep.setTopK(2);
        rep.recordEpisode("low", Arrays.asList(1L), 0.1);
        rep.recordEpisode("mid", Arrays.asList(2L), 0.5);
        rep.recordEpisode("hi",  Arrays.asList(3L), 0.9);
        List<ReplaySignal> out = rep.emitTopK();
        assertEquals(2, out.size());
        assertEquals("hi", out.get(0).getSequenceId());
        assertEquals("mid", out.get(1).getSequenceId());
    }

    // ---- SharpWaveRippleNeuron ----

    @Test
    void swr_onlyEmitsInNREM3WithSufficientDepth() {
        SharpWaveRippleNeuron swr = new SharpWaveRippleNeuron();
        List<Long> seq = Arrays.asList(1L, 2L, 3L);
        assertNull(swr.maybeEmit(SleepPhase.WAKE, 1.0, seq, 0.8), "no emit in WAKE");
        assertNull(swr.maybeEmit(SleepPhase.NREM2, 1.0, seq, 0.8), "no emit in NREM2");
        assertNull(swr.maybeEmit(SleepPhase.NREM3, 0.3, seq, 0.8), "too shallow");
        SharpWaveRippleSignal s = swr.maybeEmit(SleepPhase.NREM3, 0.9, seq, 0.8);
        assertNotNull(s);
        assertEquals(seq, s.getNeuronSequence());
    }

    // ---- REMDreamingNeuron ----

    @Test
    void dream_onlyDuringREM() {
        REMDreamingNeuron d = new REMDreamingNeuron();
        List<List<Long>> eps = Arrays.asList(Arrays.asList(1L, 2L), Arrays.asList(3L, 4L));
        assertTrue(d.recombine(SleepPhase.WAKE, eps).isEmpty());
        assertTrue(d.recombine(SleepPhase.NREM3, eps).isEmpty());
        assertFalse(d.recombine(SleepPhase.REM, eps).isEmpty());
    }

    @Test
    void dream_safetyGateRejectsHighNovelty() {
        REMDreamingNeuron d = new REMDreamingNeuron();
        d.setMaxNoveltyForPlanning(0.5);
        DreamSignal safe = new DreamSignal(new ArrayList<>(), 0.2);
        DreamSignal unsafe = new DreamSignal(new ArrayList<>(), 0.9);
        assertTrue(d.isPlanningCandidate(safe));
        assertFalse(d.isPlanningCandidate(unsafe));
    }

    // ---- Signals ----

    @Test
    void replaySignal_processingFrequency() {
        assertEquals(3L, ReplaySignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(2, ReplaySignal.PROCESSING_FREQUENCY.getLoop());
    }

    @Test
    void sleepStateSignal_copyPreservesFields() {
        SleepStateSignal s = new SleepStateSignal(SleepPhase.NREM3, 0.9);
        SleepStateSignal c = (SleepStateSignal) s.copySignal();
        assertEquals(SleepPhase.NREM3, c.getPhase());
        assertEquals(0.9, c.getDepth(), 1e-9);
    }

    @Test
    void dreamSignal_clampsNovelty() {
        DreamSignal d = new DreamSignal(Arrays.asList(1L), 2.0);
        assertEquals(1.0, d.getNoveltyScore(), 1e-9);
        d.setNoveltyScore(-0.5);
        assertEquals(0.0, d.getNoveltyScore(), 1e-9);
    }

    @Test
    void sharpWaveRippleSignal_processingFrequency() {
        assertEquals(1L, SharpWaveRippleSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(2, SharpWaveRippleSignal.PROCESSING_FREQUENCY.getLoop());
    }

    // ---- Config ----

    @Test
    void sleepConfig_defaultsAreDisabled() {
        SleepConfig cfg = new SleepConfig();
        assertFalse(cfg.isEnabled());
        assertEquals(10000, cfg.getCircadian().getCycleTicks());
        assertEquals(0.6, cfg.getCircadian().getNremFraction());
        assertEquals(0.15, cfg.getCircadian().getRemFraction());
        assertEquals(ReplayDirection.REVERSE, cfg.getReplay().getDirection());
        assertEquals(10.0, cfg.getReplay().getCompressionRatio());
        assertEquals(20, cfg.getReplay().getTopKEpisodes());
        assertEquals(5, cfg.getDreaming().getRecombinationCount());
        assertEquals(0.7, cfg.getDreaming().getMaxNoveltyForPlanning());
        assertEquals(3.0, cfg.getConsolidationBoost());
    }
}
