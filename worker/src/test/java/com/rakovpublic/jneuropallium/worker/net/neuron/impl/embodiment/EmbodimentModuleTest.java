/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.embodiment;

import com.rakovpublic.jneuropallium.ai.signals.fast.MotorCommandSignal;
import com.rakovpublic.jneuropallium.ai.signals.slow.HarmFeedbackSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.BodySchemaUpdateSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.EfferenceCopySignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.ProprioceptiveSignal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmbodimentModuleTest {

    // ---- EfferenceCopyNeuron ----

    @Test
    void efferenceCopyNeuron_producesCopyWithMatchingCommandId() {
        EfferenceCopyNeuron n = new EfferenceCopyNeuron();
        n.setId(1L);
        MotorCommandSignal cmd = new MotorCommandSignal(0, new double[]{0.1, 0.2, 0.3});
        EfferenceCopySignal copy1 = n.produceCopy(cmd);
        EfferenceCopySignal copy2 = n.produceCopy(cmd);
        assertNotNull(copy1);
        assertNotNull(copy2);
        assertNotEquals(copy1.getOriginalMotorCommandId(), copy2.getOriginalMotorCommandId());
        assertArrayEquals(new double[]{0.1, 0.2, 0.3}, copy1.getPredictedOutcome(), 1e-9);
        assertEquals(0, copy1.getEffectorId());
    }

    @Test
    void efferenceCopyNeuron_nullMotorReturnsNull() {
        EfferenceCopyNeuron n = new EfferenceCopyNeuron();
        assertNull(n.produceCopy(null));
    }

    // ---- ReafferenceComparatorNeuron ----

    @Test
    void reafferenceMismatch_producesHarmFeedbackAboveThreshold() {
        ReafferenceComparatorNeuron cmp = new ReafferenceComparatorNeuron();
        cmp.setFailureEmitThreshold(0.1);
        EfferenceCopySignal expected = new EfferenceCopySignal(1L, new double[]{1.0, 1.0}, 0);
        cmp.registerEfferenceCopy(expected);
        cmp.onProprioceptive(new ProprioceptiveSignal(0, new double[]{0.2, 0.2}, 100L));
        HarmFeedbackSignal fb = cmp.maybeEmitFailure("plan-x");
        assertNotNull(fb);
        assertTrue(fb.isActualHarmOccurred());
        assertEquals("mechanical", fb.getFeedbackSource());
        assertEquals("plan-x", fb.getActionPlanId());
    }

    @Test
    void reafferenceSmallMismatch_doesNotEmit() {
        ReafferenceComparatorNeuron cmp = new ReafferenceComparatorNeuron();
        cmp.setFailureEmitThreshold(0.5);
        cmp.registerEfferenceCopy(new EfferenceCopySignal(2L, new double[]{1.0}, 0));
        cmp.onProprioceptive(new ProprioceptiveSignal(0, new double[]{0.99}, 1L));
        assertNull(cmp.maybeEmitFailure("plan-y"));
    }

    // ---- BodySchemaNeuron + ToolIncorporationNeuron ----

    @Test
    void toolIncorporation_extendsAndRestoresBodySchema() {
        BodySchemaNeuron body = new BodySchemaNeuron();
        EffectorCapability arm = new EffectorCapability(7, new double[7], new double[7], 1.0, false);
        body.register(0, arm);

        ToolIncorporationNeuron tool = new ToolIncorporationNeuron();
        EffectorCapability hammer = new EffectorCapability(2, new double[2], new double[]{1, 1}, 1.0, false);
        BodySchemaUpdateSignal sig = tool.incorporate(body, 99, hammer);
        assertNotNull(sig);
        assertTrue(sig.isTool());
        assertTrue(body.currentSchema().has(99));
        assertEquals(2, body.currentSchema().get(99).getDof());

        tool.release(body, 99);
        assertFalse(body.currentSchema().has(99),
                "releasing a tool that had no prior effector should remove the slot");
        assertTrue(body.currentSchema().has(0), "arm remains");
    }

    @Test
    void bodySchemaNeuron_registersNewEffectorOnProprioception() {
        BodySchemaNeuron body = new BodySchemaNeuron();
        body.onProprioceptive(new ProprioceptiveSignal(5, new double[]{0.1, 0.2}, 1L));
        assertTrue(body.currentSchema().has(5));
        assertEquals(2, body.currentSchema().get(5).getDof());
    }

    // ---- Signals ----

    @Test
    void proprioceptiveSignal_copyPreservesFields() {
        ProprioceptiveSignal s = new ProprioceptiveSignal(1, new double[]{0.1, 0.2}, 77L);
        ProprioceptiveSignal c = (ProprioceptiveSignal) s.copySignal();
        assertEquals(1, c.getEffectorId());
        assertArrayEquals(new double[]{0.1, 0.2}, c.getJointStates(), 1e-9);
        assertEquals(77L, c.getTimestamp());
    }

    @Test
    void efferenceCopySignal_processingFrequency() {
        assertEquals(1L, EfferenceCopySignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(1, EfferenceCopySignal.PROCESSING_FREQUENCY.getLoop());
    }

    @Test
    void bodySchemaUpdateSignal_processingFrequency() {
        assertEquals(3L, BodySchemaUpdateSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(2, BodySchemaUpdateSignal.PROCESSING_FREQUENCY.getLoop());
    }

    // ---- Config ----

    @Test
    void embodimentConfig_defaultsAreDisabled() {
        EmbodimentConfig cfg = new EmbodimentConfig();
        assertFalse(cfg.isEnabled());
        assertEquals(0.15, cfg.getEfferenceCopyMismatchThreshold());
        assertEquals(0.4, cfg.getEfferenceCopyFailureEmitThreshold());
        assertTrue(cfg.isToolIncorporationEnabled());
        assertEquals(600, cfg.getToolIncorporationTimeoutTicks());
    }
}
