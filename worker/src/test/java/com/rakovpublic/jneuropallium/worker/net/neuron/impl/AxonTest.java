package com.rakovpublic.jneuropallium.worker.net.neuron.impl;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISynapse;
import com.rakovpublic.jneuropallium.worker.net.neuron.IWeight;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AxonTest {

    private Axon axon;

    @Mock
    private IWeight mockWeight;

    @Mock
    private ISignal mockSignal;

    @BeforeEach
    void setUp() {
        axon = new Axon();
    }

    // ─── putConnection ───────────────────────────────────────────────────────

    @Test
    void testPutConnection_newSignalClass_createsEntry() {
        NeuronSynapse synapse = NeuronSynapse.createConnection(2, 1, 100L, 10L, mockWeight, "desc");
        axon.putConnection(ISignal.class, synapse);

        assertTrue(axon.getConnectionMap().containsKey(ISignal.class));
        assertEquals(1, axon.getConnectionMap().get(ISignal.class).size());
    }

    @Test
    void testPutConnection_existingSignalClass_appendsEntry() {
        NeuronSynapse s1 = NeuronSynapse.createConnection(2, 1, 100L, 10L, mockWeight, "");
        NeuronSynapse s2 = NeuronSynapse.createConnection(2, 1, 101L, 10L, mockWeight, "");
        axon.putConnection(ISignal.class, s1);
        axon.putConnection(ISignal.class, s2);

        assertEquals(2, axon.getConnectionMap().get(ISignal.class).size());
    }

    // ─── processSignals ──────────────────────────────────────────────────────

    @Test
    void testProcessSignals_noMatchingConnection_returnsEmptyMap() {
        when(mockSignal.getCurrentSignalClass()).thenReturn((Class) ISignal.class);
        List<ISignal> signals = List.of(mockSignal);

        HashMap<ISignal, List<ISynapse>> result = axon.processSignals(signals);

        assertTrue(result.isEmpty());
    }

    @Test
    void testProcessSignals_withMatchingConnection_returnsProcessedEntry() {
        when(mockSignal.getCurrentSignalClass()).thenReturn((Class) ISignal.class);
        when(mockWeight.process(mockSignal)).thenReturn(mockSignal);

        NeuronSynapse synapse = NeuronSynapse.createConnection(2, 1, 100L, 10L, mockWeight, "");
        axon.putConnection(ISignal.class, synapse);

        HashMap<ISignal, List<ISynapse>> result = axon.processSignals(List.of(mockSignal));

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    // ─── destroyConnection ───────────────────────────────────────────────────

    @Test
    void testDestroyConnection_removesCorrectSynapse() {
        when(mockWeight.getSignalClass()).thenReturn((Class) ISignal.class);

        NeuronSynapse synapse = NeuronSynapse.createConnection(2, 1, 100L, 10L, mockWeight, "");
        axon.putConnection(ISignal.class, synapse);

        // Verify it was added
        assertEquals(1, axon.getConnectionMap().get(ISignal.class).size());

        // Destroy connection - no ConcurrentModificationException should be thrown
        assertDoesNotThrow(() -> axon.destroyConnection(2, 100L, ISignal.class));

        // Should be removed
        assertTrue(axon.getConnectionMap().get(ISignal.class).isEmpty());
    }

    @Test
    void testDestroyConnection_nonExistentLayer_doesNotThrow() {
        assertDoesNotThrow(() -> axon.destroyConnection(99, 999L, ISignal.class));
    }

    @Test
    void testDestroyConnection_nonExistentNeuron_doesNotThrow() {
        NeuronSynapse synapse = NeuronSynapse.createConnection(2, 1, 100L, 10L, mockWeight, "");
        axon.putConnection(ISignal.class, synapse);

        assertDoesNotThrow(() -> axon.destroyConnection(2, 999L, ISignal.class));

        // Original synapse should remain untouched
        assertEquals(1, axon.getConnectionMap().get(ISignal.class).size());
    }

    @Test
    void testDestroyConnection_doesNotThrowConcurrentModification() {
        when(mockWeight.getSignalClass()).thenReturn((Class) ISignal.class);

        // Add multiple synapses for same address
        NeuronSynapse s1 = NeuronSynapse.createConnection(2, 1, 100L, 10L, mockWeight, "");
        NeuronSynapse s2 = NeuronSynapse.createConnection(2, 1, 100L, 10L, mockWeight, "");
        axon.putConnection(ISignal.class, s1);
        axon.putConnection(ISignal.class, s2);

        assertDoesNotThrow(() -> axon.destroyConnection(2, 100L, ISignal.class));
    }

    // ─── cleanConnections ────────────────────────────────────────────────────

    @Test
    void testCleanConnections_clearsAll() {
        NeuronSynapse synapse = NeuronSynapse.createConnection(2, 1, 100L, 10L, mockWeight, "");
        axon.putConnection(ISignal.class, synapse);

        axon.cleanConnections();

        assertTrue(axon.getConnectionMap().isEmpty());
    }

    // ─── resetConnection ─────────────────────────────────────────────────────

    @Test
    void testResetConnection_replacesConnectionMap() {
        NeuronSynapse old = NeuronSynapse.createConnection(2, 1, 100L, 10L, mockWeight, "");
        axon.putConnection(ISignal.class, old);

        HashMap<Class<? extends ISignal>, List<ISynapse>> newMap = new HashMap<>();
        axon.resetConnection(newMap);

        assertSame(newMap, axon.getConnectionMap());
    }
}
