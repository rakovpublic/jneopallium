package com.rakovpublic.jneuropallium.worker.net.neuron.impl;

import com.rakovpublic.jneuropallium.worker.net.neuron.IWeight;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.inmemory.TestSignal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DendritesTest {

    private Dendrites dendrites;

    @Mock
    private IWeight mockWeight;

    @BeforeEach
    void setUp() {
        dendrites = new Dendrites();
    }

    // ─── updateWeight ────────────────────────────────────────────────────────

    @Test
    void testUpdateWeight_newAddress_createsEntry() {
        NeuronAddress addr = new NeuronAddress(1, 10L);
        dendrites.updateWeight(addr, ISignal.class, mockWeight);

        assertTrue(dendrites.getWeights().containsKey(addr));
        assertEquals(1, dendrites.getWeights().get(addr).size());
    }

    @Test
    void testUpdateWeight_existingAddress_appends() {
        NeuronAddress addr = new NeuronAddress(1, 10L);
        IWeight second = mock(IWeight.class);
        dendrites.updateWeight(addr, ISignal.class, mockWeight);
        dendrites.updateWeight(addr, ISignal.class, second);

        assertEquals(2, dendrites.getWeights().get(addr).size());
    }

    // ─── removeAllWeights ────────────────────────────────────────────────────

    @Test
    void testRemoveAllWeights_removesEntry() {
        NeuronAddress addr = new NeuronAddress(1, 10L);
        dendrites.updateWeight(addr, ISignal.class, mockWeight);
        dendrites.removeAllWeights(addr);

        assertFalse(dendrites.getWeights().containsKey(addr));
    }

    // ─── removeWeightForClass ────────────────────────────────────────────────

    @Test
    void testRemoveWeightForClass_removesCorrectWeight() {
        NeuronAddress addr = new NeuronAddress(1, 10L);
        when(mockWeight.getSignalClass()).thenReturn((Class) TestSignal.class);
        dendrites.updateWeight(addr, TestSignal.class, mockWeight);

        // This used to be broken (used == and removed wrong element); now fixed
        assertDoesNotThrow(() -> dendrites.removeWeightForClass(addr, TestSignal.class));

        assertTrue(dendrites.getWeights().get(addr).isEmpty(),
                "Weight should be removed from the list");
    }

    @Test
    void testRemoveWeightForClass_leavesOtherWeights() {
        NeuronAddress addr = new NeuronAddress(1, 10L);
        IWeight otherWeight = mock(IWeight.class);

        when(mockWeight.getSignalClass()).thenReturn((Class) TestSignal.class);
        when(otherWeight.getSignalClass()).thenReturn((Class) ISignal.class);

        dendrites.updateWeight(addr, TestSignal.class, mockWeight);
        dendrites.updateWeight(addr, ISignal.class, otherWeight);

        dendrites.removeWeightForClass(addr, TestSignal.class);

        assertEquals(1, dendrites.getWeights().get(addr).size());
        assertSame(otherWeight, dendrites.getWeights().get(addr).get(0));
    }

    @Test
    void testRemoveWeightForClass_unknownAddress_doesNotThrow() {
        NeuronAddress addr = new NeuronAddress(99, 999L);
        assertDoesNotThrow(() -> dendrites.removeWeightForClass(addr, ISignal.class));
    }

    // ─── processSignalsWithDendrites ─────────────────────────────────────────

    @Test
    void testProcessSignalsWithDendrites_emptyList_returnsEmpty() {
        List<ISignal> result = dendrites.processSignalsWithDendrites(new ArrayList<>());
        assertTrue(result.isEmpty());
    }

    @Test
    void testProcessSignalsWithDendrites_withDefaultWeight_appliesWeight() {
        TestSignal signal = new TestSignal(2, false);
        TestSignal processed = new TestSignal(2, false);

        HashMap<Class<? extends ISignal>, IWeight> defaultWeights = new HashMap<>();
        when(mockWeight.process(signal)).thenReturn(processed);
        defaultWeights.put(TestSignal.class, mockWeight);
        dendrites.setDefaultDendritesWeights(defaultWeights);

        List<ISignal> result = dendrites.processSignalsWithDendrites(List.of(signal));

        assertEquals(1, result.size());
        assertSame(processed, result.get(0));
    }

    @Test
    void testProcessSignalsWithDendrites_noWeightForSignal_returnsOriginal() {
        TestSignal signal = new TestSignal(2, false);
        // No default weight registered for TestSignal

        List<ISignal> result = dendrites.processSignalsWithDendrites(List.of(signal));

        assertEquals(1, result.size());
        assertSame(signal, result.get(0));
    }

    @Test
    void testProcessSignalsWithDendrites_multipleSignals_allProcessed() {
        TestSignal s1 = new TestSignal(2, false);
        TestSignal s2 = new TestSignal(3, false);

        List<ISignal> result = dendrites.processSignalsWithDendrites(List.of(s1, s2));

        assertEquals(2, result.size());
    }

    // ─── moveConnection ──────────────────────────────────────────────────────

    @Test
    void testMoveConnection_removesAddressesForLayer() {
        NeuronAddress addr1 = new NeuronAddress(1, 10L);
        NeuronAddress addr2 = new NeuronAddress(2, 20L);
        dendrites.updateWeight(addr1, ISignal.class, mockWeight);
        dendrites.updateWeight(addr2, ISignal.class, mockWeight);

        // Use LayerMove pointing to remove layer 1
        com.rakovpublic.jneuropallium.worker.net.layers.impl.LayerMove layerMove =
                mock(com.rakovpublic.jneuropallium.worker.net.layers.impl.LayerMove.class);
        when(layerMove.getLayerRemoved()).thenReturn(1);

        // Should not throw ConcurrentModificationException (was a bug)
        assertDoesNotThrow(() -> dendrites.moveConnection(layerMove));

        assertFalse(dendrites.getWeights().containsKey(addr1), "Layer 1 address should be removed");
        assertTrue(dendrites.getWeights().containsKey(addr2), "Layer 2 address should remain");
    }
}
