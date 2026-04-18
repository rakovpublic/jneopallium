/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.affect;

import com.rakovpublic.jneuropallium.ai.signals.fast.SpikeSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.affect.AffectStateSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.affect.AppraisalSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.affect.InteroceptiveSignal;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.affect.AffectIntegrationProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.affect.InteroceptionProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.affect.ValenceTaggingProcessor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AffectModuleTest {

    // ---- AffectStateSignal ----

    @Test
    void affectStateSignal_serializationRoundTripPreservesFields() {
        AffectStateSignal s = new AffectStateSignal(-0.4, 0.8, "ctx-1");
        AffectStateSignal copy = (AffectStateSignal) s.copySignal();
        assertEquals(-0.4, copy.getValence(), 1e-9);
        assertEquals(0.8, copy.getArousal(), 1e-9);
        assertEquals("ctx-1", copy.getContextId());
    }

    @Test
    void affectStateSignal_processingFrequencyIsLoop2Epoch1() {
        assertEquals(1L, AffectStateSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(2, AffectStateSignal.PROCESSING_FREQUENCY.getLoop());
        AffectStateSignal s = new AffectStateSignal(0, 0, "c");
        assertEquals(2, s.getLoop());
        assertEquals(1L, s.getEpoch());
    }

    @Test
    void affectStateSignal_clampsValenceAndArousalInConstructor() {
        AffectStateSignal s = new AffectStateSignal(5.0, -2.0, "ctx");
        assertEquals(1.0, s.getValence(), 1e-9);
        assertEquals(0.0, s.getArousal(), 1e-9);
    }

    @Test
    void interoceptiveSignal_processingFrequencyIsLoop1Epoch2() {
        assertEquals(2L, InteroceptiveSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(1, InteroceptiveSignal.PROCESSING_FREQUENCY.getLoop());
    }

    @Test
    void appraisalSignal_processingFrequencyIsLoop1Epoch2() {
        assertEquals(2L, AppraisalSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(1, AppraisalSignal.PROCESSING_FREQUENCY.getLoop());
    }

    // ---- AmygdalaValenceNeuron ----

    @Test
    void amygdalaValenceNeuron_threatPositiveInputProducesNegativeValence() {
        AmygdalaValenceNeuron n = new AmygdalaValenceNeuron();
        double v = n.tag(0.9, 0.9);
        assertTrue(v < 0, "threat-positive input should yield negative valence, got " + v);
        assertTrue(n.currentState().getArousal() > 0.5);
    }

    @Test
    void amygdalaValenceNeuron_modulateThresholdDecreasesWithArousal() {
        AmygdalaValenceNeuron n = new AmygdalaValenceNeuron();
        double base = n.getFiringThreshold();
        n.modulateThreshold(1.0);
        assertTrue(n.getFiringThreshold() < base);
    }

    // ---- ValenceTaggingProcessor ----

    @Test
    void valenceTaggingProcessor_forwardsSpikeToAmygdala() {
        AmygdalaValenceNeuron n = new AmygdalaValenceNeuron();
        ValenceTaggingProcessor p = new ValenceTaggingProcessor();
        SpikeSignal spike = new SpikeSignal(true, 1.2, 5);
        List<?> out = p.process(spike, n);
        assertTrue(out.isEmpty());
        assertTrue(n.currentState().getArousal() > 0);
    }

    // ---- AnteriorInsulaNeuron + InteroceptionProcessor ----

    @Test
    void anteriorInsula_integratesInteroceptiveSignals() {
        AnteriorInsulaNeuron n = new AnteriorInsulaNeuron();
        InteroceptionProcessor p = new InteroceptionProcessor();
        p.process(new InteroceptiveSignal(0.8, 0.5, 0.2, "energy"), n);
        p.process(new InteroceptiveSignal(0.7, 0.6, 0.3, "energy"), n);
        assertTrue(n.getSampleCount() >= 2);
        assertTrue(n.readHomeostaticError() > 0);
        assertTrue(n.readEnergyBudget() > 0);
    }

    // ---- AffectIntegrationNeuron + AffectIntegrationProcessor ----

    @Test
    void affectIntegrationProcessor_emitsAffectStateSignalForIntegrationNeuron() {
        AffectIntegrationNeuron n = new AffectIntegrationNeuron();
        n.setId(42L);
        AffectIntegrationProcessor p = new AffectIntegrationProcessor();
        List<?> out = p.process(new AppraisalSignal(0.5, 0.7, 0.4), n);
        assertEquals(1, out.size());
        assertTrue(out.get(0) instanceof AffectStateSignal);
        AffectStateSignal emitted = (AffectStateSignal) out.get(0);
        assertEquals(42L, emitted.getSourceNeuronId());
    }

    // ---- AffectModulationNeuron: integration test for harm threshold tightening ----

    @Test
    void affectModulation_negativeValenceTightensHarmThreshold() {
        AffectModulationNeuron mod = new AffectModulationNeuron();
        mod.onAffect(new AffectStateSignal(-0.8, 0.7, "ctx"));
        assertTrue(mod.getHarmThresholdMultiplier() > 1.0,
                "negative valence should raise harm threshold multiplier above baseline, got "
                        + mod.getHarmThresholdMultiplier());
    }

    @Test
    void affectModulation_scalesLearningRatesWithArousal() {
        AffectModulationNeuron mod = new AffectModulationNeuron();
        mod.onAffect(new AffectStateSignal(0.0, 0.6, "ctx"));
        assertEquals(1.6, mod.getShortTermLearningScale(), 1e-9);
        assertEquals(0.7, mod.getLongTermConsolidationScale(), 1e-9);
    }

    @Test
    void affectModulation_respectsHarmClampRange() {
        AffectModulationNeuron mod = new AffectModulationNeuron();
        mod.onAffect(new AffectStateSignal(-1.0, 1.0, "ctx")); // raw = 2.0, within [1,5]
        assertTrue(mod.getHarmThresholdMultiplier() >= mod.getHarmClampMin());
        assertTrue(mod.getHarmThresholdMultiplier() <= mod.getHarmClampMax());
    }

    // ---- AffectConfig backwards-compat property ----

    @Test
    void affectConfig_defaultsAreDisabled() {
        AffectConfig cfg = new AffectConfig();
        assertFalse(cfg.isEnabled(), "affect module must default to disabled");
        assertEquals(300, cfg.getValenceDecayTicks());
        assertEquals(150, cfg.getArousalDecayTicks());
        assertEquals(1.0, cfg.getHarmThresholdClampMin());
        assertEquals(5.0, cfg.getHarmThresholdClampMax());
    }
}
