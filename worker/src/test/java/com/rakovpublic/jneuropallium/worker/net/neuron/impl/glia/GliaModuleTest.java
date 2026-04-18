/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.glia;

import com.rakovpublic.jneuropallium.worker.net.signals.impl.glia.CalciumWaveSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.glia.GliotransmitterSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.glia.MyelinationSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.glia.PruningSignal;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GliaModuleTest {

    // ---- DelayedAxon ----

    @Test
    void delayedAxon_setDelayClampsToBaselineAndMin() {
        DelayedAxon a = new DelayedAxon(5, 1);
        a.setDelay(42L, 99);
        assertEquals(5, a.getDelay(42L), "clamps above baseline");
        a.setDelay(42L, -3);
        assertEquals(1, a.getDelay(42L), "clamps below min");
    }

    @Test
    void delayedAxon_accelerateMonotonicallyDecreasesToFloor() {
        DelayedAxon a = new DelayedAxon(5, 1);
        a.setDelay(7L, 5);
        int d1 = a.accelerate(7L, 1);
        int d2 = a.accelerate(7L, 1);
        int d3 = a.accelerate(7L, 100);
        assertEquals(4, d1);
        assertEquals(3, d2);
        assertEquals(1, d3, "accelerate cannot go below min");
    }

    @Test
    void delayedAxon_demyelinateRestoresBaselineAndNeverAbove() {
        DelayedAxon a = new DelayedAxon(5, 1);
        a.setDelay(7L, 1);
        int restored = a.demyelinate(7L);
        assertEquals(5, restored);
        assertEquals(5, a.getDelay(7L));
    }

    // ---- DelayQueue ----

    @Test
    void delayQueue_releasesAtExactTick() {
        DelayQueue q = new DelayQueue();
        CalciumWaveSignal s = new CalciumWaveSignal(1, 0.5, 1.0);
        q.enqueue(100L, 7L, s, 3);
        assertTrue(q.releaseReady(101L).isEmpty(), "not yet");
        assertTrue(q.releaseReady(102L).isEmpty(), "still not yet");
        List<DelayQueue.ReleasedSignal> ready = q.releaseReady(103L);
        assertEquals(1, ready.size());
        assertEquals(7L, ready.get(0).targetNeuronId);
        assertSame(s, ready.get(0).signal);
    }

    @Test
    void delayQueue_zeroDelayReleasesImmediately() {
        DelayQueue q = new DelayQueue();
        q.enqueue(10L, 3L, new CalciumWaveSignal(0, 0.1, 0.1), 0);
        List<DelayQueue.ReleasedSignal> ready = q.releaseReady(10L);
        assertEquals(1, ready.size());
    }

    // ---- AstrocyteNeuron ----

    @Test
    void astrocyte_emitsCalciumWaveAboveThreshold() {
        AstrocyteNeuron n = new AstrocyteNeuron(4);
        n.setCalciumWaveThreshold(1.0);
        n.accumulate(0.6);
        assertNull(n.maybeEmitWave(), "below threshold");
        n.accumulate(0.6);
        CalciumWaveSignal w = n.maybeEmitWave();
        assertNotNull(w);
        assertEquals(4, w.getRegionId());
        assertEquals(0, n.getIntegratedActivity(), 1e-9, "integrator cleared");
    }

    @Test
    void astrocyte_releasesGliotransmitter() {
        AstrocyteNeuron n = new AstrocyteNeuron(2);
        GliotransmitterSignal g = n.release(GliotransmitterType.D_SERINE, 0.7);
        assertEquals(GliotransmitterType.D_SERINE, g.getTransmitter());
        assertEquals(0.7, g.getConcentration(), 1e-9);
        assertEquals(2, g.getRegionId());
    }

    // ---- MicroglialPruningNeuron ----

    @Test
    void microglia_pruningSafetyRespectsInactivityWindow() {
        MicroglialPruningNeuron n = new MicroglialPruningNeuron();
        n.setMinInactivityTicks(100);
        for (int i = 0; i < 50; i++) n.tick();
        n.recordActivity(1L, 2L);
        for (int i = 0; i < 10; i++) n.tick();
        assertNull(n.maybePrune(1L, 2L, null), "cannot prune recently-active");
        for (int i = 0; i < 100; i++) n.tick();
        assertNotNull(n.maybePrune(1L, 2L, null), "allowed after silence");
    }

    @Test
    void microglia_honorsEpochCap() {
        MicroglialPruningNeuron n = new MicroglialPruningNeuron();
        n.setMinInactivityTicks(1);
        n.setMaxPruningsPerEpoch(2);
        for (int i = 0; i < 5; i++) n.tick();
        assertNotNull(n.maybePrune(1L, 10L, null));
        assertNotNull(n.maybePrune(1L, 11L, null));
        assertNull(n.maybePrune(1L, 12L, null), "cap enforced");
        n.resetEpochCounter();
        assertNotNull(n.maybePrune(1L, 12L, null), "after reset it emits");
    }

    // ---- MyelinationNeuron ----

    @Test
    void myelination_reducesDelayMonotonicallyToFloor() {
        MyelinationNeuron n = new MyelinationNeuron();
        n.setBaselineDelayTicks(5);
        n.setMinDelayTicks(1);
        n.setUsageThreshold(3);

        DelayedAxon axon = new DelayedAxon(5, 1);
        axon.setDelay(99L, 5);

        int prev = 5;
        for (int w = 0; w < 10; w++) {
            for (int i = 0; i < 5; i++) n.recordUsage(99L);
            MyelinationSignal sig = n.evaluate(1L, 99L);
            if (sig != null) {
                n.applyTo(axon, sig);
                assertTrue(sig.getNewDelayTicks() <= prev, "monotonic decrease");
                prev = sig.getNewDelayTicks();
            }
        }
        assertEquals(1, axon.getDelay(99L));
    }

    @Test
    void myelination_demyelinationRestoresBaselineNotAbove() {
        MyelinationNeuron n = new MyelinationNeuron();
        n.setBaselineDelayTicks(4);
        n.setMinDelayTicks(1);
        DelayedAxon axon = new DelayedAxon(4, 1);
        axon.setDelay(5L, 1);
        n.demyelinate(axon, 5L);
        assertEquals(4, axon.getDelay(5L), "restored to baseline, not above");
    }

    // ---- Signals ----

    @Test
    void pruningSignal_processingFrequency() {
        assertEquals(5L, PruningSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(2, PruningSignal.PROCESSING_FREQUENCY.getLoop());
    }

    @Test
    void myelinationSignal_processingFrequency() {
        assertEquals(10L, MyelinationSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(2, MyelinationSignal.PROCESSING_FREQUENCY.getLoop());
    }

    @Test
    void calciumWaveSignal_copyPreservesFields() {
        CalciumWaveSignal s = new CalciumWaveSignal(3, 0.8, 2.0);
        CalciumWaveSignal c = (CalciumWaveSignal) s.copySignal();
        assertEquals(3, c.getRegionId());
        assertEquals(0.8, c.getAmplitude(), 1e-9);
        assertEquals(2.0, c.getPropagationRadius(), 1e-9);
    }

    // ---- Config ----

    @Test
    void gliaConfig_defaultsAreDisabled() {
        GliaConfig cfg = new GliaConfig();
        assertFalse(cfg.isEnabled());
        assertTrue(cfg.getAstrocytes().isPerLayer());
        assertEquals(0.4, cfg.getAstrocytes().getCalciumWaveThreshold());
        assertTrue(cfg.getMicroglia().isPruningEnabled());
        assertEquals(2000, cfg.getMicroglia().getMinInactivityTicks());
        assertTrue(cfg.getMyelination().isEnabled());
        assertEquals(5, cfg.getMyelination().getBaselineDelayTicks());
        assertEquals(1, cfg.getMyelination().getMinDelayTicks());
    }
}
