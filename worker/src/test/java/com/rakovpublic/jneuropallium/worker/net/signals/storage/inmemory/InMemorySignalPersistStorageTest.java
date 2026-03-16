package com.rakovpublic.jneuropallium.worker.net.signals.storage.inmemory;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

class InMemorySignalPersistStorageTest {

    private InMemorySignalPersistStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemorySignalPersistStorage();
    }

    private HashMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> buildInput(
            int layerId, long neuronId, ISignal... signals) {
        CopyOnWriteArrayList<ISignal> list = new CopyOnWriteArrayList<>();
        for (ISignal s : signals) list.add(s);
        HashMap<Long, CopyOnWriteArrayList<ISignal>> neuronMap = new HashMap<>();
        neuronMap.put(neuronId, list);
        HashMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> result = new HashMap<>();
        result.put(layerId, neuronMap);
        return result;
    }

    // ─── putSignals / getLayerSignals ────────────────────────────────────────

    @Test
    void testPutSignals_newLayer() {
        TestSignal s = new TestSignal(3, false);
        storage.putSignals(buildInput(1, 10L, s));

        assertNotNull(storage.getLayerSignals(1));
        assertEquals(1, storage.getLayerSignals(1).get(10L).size());
    }

    @Test
    void testPutSignals_mergesExistingNeuron() {
        TestSignal s1 = new TestSignal(3, false);
        TestSignal s2 = new TestSignal(2, false);
        storage.putSignals(buildInput(1, 10L, s1));
        storage.putSignals(buildInput(1, 10L, s2));

        assertEquals(2, storage.getLayerSignals(1).get(10L).size());
    }

    @Test
    void testPutSignals_addsNewNeuronToExistingLayer() {
        storage.putSignals(buildInput(1, 10L, new TestSignal(3, false)));
        storage.putSignals(buildInput(1, 20L, new TestSignal(2, false)));

        assertEquals(2, storage.getLayerSignals(1).size());
    }

    @Test
    void testGetLayerSignals_unknownLayer_returnsNull() {
        assertNull(storage.getLayerSignals(99));
    }

    // ─── hasSignalsToProcess ─────────────────────────────────────────────────

    @Test
    void testHasSignalsToProcess_true() {
        storage.putSignals(buildInput(1, 10L, new TestSignal(3, false)));
        assertTrue(storage.hasSignalsToProcess());
    }

    @Test
    void testHasSignalsToProcess_false_whenEmpty() {
        assertFalse(storage.hasSignalsToProcess());
    }

    // ─── cleanOutdatedSignals ────────────────────────────────────────────────

    @Test
    void testCleanOutdatedSignals_removesExpiredSignals() {
        TestSignal alive = new TestSignal(2, false);
        TestSignal dead = new TestSignal(0, false);
        storage.putSignals(buildInput(1, 10L, alive, dead));

        // No ConcurrentModificationException
        assertDoesNotThrow(() -> storage.cleanOutdatedSignals());

        CopyOnWriteArrayList<ISignal> remaining = storage.getLayerSignals(1).get(10L);
        assertEquals(1, remaining.size(), "Dead signal (timeAlive=0) should be removed");
    }

    @Test
    void testCleanOutdatedSignals_decrementsTimeAliveForSurvivors() {
        TestSignal alive = new TestSignal(3, false);
        storage.putSignals(buildInput(1, 10L, alive));

        storage.cleanOutdatedSignals();

        ISignal remaining = storage.getLayerSignals(1).get(10L).get(0);
        assertEquals(2, remaining.getTimeAlive(),
                "Surviving signal's timeAlive should be decremented");
    }

    @Test
    void testCleanOutdatedSignals_noExceptionWithEmptyList() {
        storage.putSignals(buildInput(1, 10L)); // empty list for neuron
        assertDoesNotThrow(() -> storage.cleanOutdatedSignals());
    }

    @Test
    void testCleanOutdatedSignals_allDead_listBecomesEmpty() {
        storage.putSignals(buildInput(1, 10L, new TestSignal(0, false), new TestSignal(0, false)));

        storage.cleanOutdatedSignals();

        assertTrue(storage.getLayerSignals(1).get(10L).isEmpty());
    }

    // ─── cleanMiddleLayerSignals ─────────────────────────────────────────────

    @Test
    void testCleanMiddleLayerSignals_removesNeedToRemoveSignals() {
        // Layer 1 is first (skipped by 'first' flag), layer 2 is the second entry
        storage.putSignals(buildInput(1, 10L, new TestSignal(3, false)));
        storage.putSignals(buildInput(2, 20L, new TestSignal(3, true)));  // needToRemove=true

        assertDoesNotThrow(() -> storage.cleanMiddleLayerSignals());

        assertEquals(1, storage.getLayerSignals(1).get(10L).size(), "First layer signals unchanged");
        assertTrue(storage.getLayerSignals(2).get(20L).isEmpty(), "needToRemove signal should be gone");
    }

    @Test
    void testCleanMiddleLayerSignals_removesDeadSignals() {
        storage.putSignals(buildInput(1, 10L, new TestSignal(3, false)));
        storage.putSignals(buildInput(2, 20L, new TestSignal(0, false)));  // timeAlive=0

        storage.cleanMiddleLayerSignals();

        assertTrue(storage.getLayerSignals(2).get(20L).isEmpty());
    }

    @Test
    void testCleanMiddleLayerSignals_skipsIntegerMinValueLayer() {
        storage.putSignals(buildInput(Integer.MIN_VALUE, 1L, new TestSignal(0, true)));

        storage.cleanMiddleLayerSignals();

        // Should not be touched
        assertEquals(1, storage.getLayerSignals(Integer.MIN_VALUE).get(1L).size());
    }

    // ─── getAllSignals / deletedLayerInput ───────────────────────────────────

    @Test
    void testGetAllSignals_returnsAll() {
        storage.putSignals(buildInput(1, 10L, new TestSignal(1, false)));
        storage.putSignals(buildInput(2, 20L, new TestSignal(1, false)));

        assertEquals(2, storage.getAllSignals().size());
    }

    @Test
    void testDeletedLayerInput_removesLayer() {
        storage.putSignals(buildInput(1, 10L, new TestSignal(1, false)));
        storage.deletedLayerInput(1);

        assertNull(storage.getLayerSignals(1));
    }
}
