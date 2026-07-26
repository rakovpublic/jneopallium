package com.rakovpublic.jneuropallium.worker.net.neuron.impl;

import com.rakovpublic.jneuropallium.worker.net.neuron.IActivationFunction;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.inmemory.TestSignal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NeuronTest {

    private Neuron neuron;

    @Mock
    private IActivationFunction<TestSignal> activationFunction;

    @BeforeEach
    void setUp() {
        neuron = new Neuron();
    }

    // ─── hasResult / activate ────────────────────────────────────────────────

    @Test
    void testHasResult_initiallyFalse() {
        assertFalse(neuron.hasResult());
    }

    @Test
    void testActivate_setsIsProcessedTrue() {
        neuron.activate();
        assertTrue(neuron.hasResult());
    }

    @Test
    void testActivate_withNoActivationFunction_keepsAllSignals() {
        TestSignal s = new TestSignal(2, false);
        neuron.result = new ArrayList<>(List.of(s));

        neuron.activate();

        assertEquals(1, neuron.result.size());
        assertSame(s, neuron.result.get(0));
    }

    @Test
    void testActivate_withActivationFunction_addsActivatedSignal_notOriginal() {
        // This is the bug fix: was adding 's' instead of 'sig.get()'
        TestSignal original = new TestSignal(2, false);
        TestSignal activated = new TestSignal(2, false); // different object

        neuron.result = new ArrayList<>(List.of(original));
        neuron.addActivationFunction(TestSignal.class, activationFunction);

        when(activationFunction.activate(original)).thenReturn(Optional.of(activated));

        neuron.activate();

        assertEquals(1, neuron.result.size());
        assertSame(activated, neuron.result.get(0),
                "activate() must add the activated signal (sig.get()), not the original");
    }

    @Test
    void testActivate_withActivationFunction_filtersEmptyOptional() {
        TestSignal original = new TestSignal(2, false);
        neuron.result = new ArrayList<>(List.of(original));
        neuron.addActivationFunction(TestSignal.class, activationFunction);

        when(activationFunction.activate(original)).thenReturn(Optional.empty());

        neuron.activate();

        assertTrue(neuron.result.isEmpty(),
                "Signal filtered by activation function (empty Optional) should not appear in result");
    }

    @Test
    void testActivate_multipleSignals_mixedActivationResults() {
        TestSignal s1 = new TestSignal(2, false);
        TestSignal s2 = new TestSignal(2, false);
        TestSignal activated1 = new TestSignal(2, false);

        neuron.result = new ArrayList<>(List.of(s1, s2));
        neuron.addActivationFunction(TestSignal.class, activationFunction);

        when(activationFunction.activate(s1)).thenReturn(Optional.of(activated1));
        when(activationFunction.activate(s2)).thenReturn(Optional.empty());

        neuron.activate();

        assertEquals(1, neuron.result.size());
        assertSame(activated1, neuron.result.get(0));
    }

    // ─── getResult ───────────────────────────────────────────────────────────

    @Test
    void testGetResult_resetsIsProcessed() {
        neuron.activate(); // sets isProcessed = true
        neuron.getResult();

        assertFalse(neuron.hasResult(), "getResult() should reset isProcessed to false");
    }

    // ─── addSignals ──────────────────────────────────────────────────────────

    @Test
    void testAddSignals_addsToSignalsList() {
        neuron.setCurrentLoop(0);
        TestSignal s = new TestSignal(2, false);

        neuron.addSignals(List.of(s));

        assertEquals(1, neuron.signals.size());
    }

    @Test
    void testAddSignals_setsCurrentInnerLoop() {
        neuron.setCurrentLoop(5);
        TestSignal s = new TestSignal(2, false);

        neuron.addSignals(List.of(s));

        assertEquals(5, neuron.signals.get(0).getCurrentInnerLoop());
    }

    // ─── canProcess ──────────────────────────────────────────────────────────

    @Test
    void testCanProcess_signal_returnsFalseWhenNoProcessor() {
        TestSignal s = new TestSignal(2, false);
        assertFalse(neuron.canProcess(s));
    }

    @Test
    void testCanProcess_class_returnsFalseWhenNoProcessor() {
        assertFalse(neuron.canProcess(TestSignal.class));
    }

    // ─── validate ────────────────────────────────────────────────────────────

    @Test
    void testValidate_noRules_returnsTrue() {
        assertTrue(neuron.validate());
    }

    // ─── isChanged / setChanged ──────────────────────────────────────────────

    @Test
    void testIsChanged_initiallyFalse() {
        assertFalse(neuron.isChanged());
    }

    @Test
    void testSetChanged_updatesFlag() {
        neuron.setChanged(true);
        assertTrue(neuron.isChanged());
    }

    // ─── isOnDelete / setOnDelete ────────────────────────────────────────────

    @Test
    void testIsOnDelete_initiallyFalse() {
        assertFalse(neuron.isOnDelete());
    }

    @Test
    void testSetOnDelete_updatesFlag() {
        neuron.setOnDelete(true);
        assertTrue(neuron.isOnDelete());
    }

    // ─── run counter ─────────────────────────────────────────────────────────

    @Test
    void testGetRun_initiallyZero() {
        assertEquals(0L, neuron.getRun());
    }

    @Test
    void testSetRun_updatesValue() {
        neuron.setRun(42L);
        assertEquals(42L, neuron.getRun());
    }
}
