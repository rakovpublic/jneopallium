package com.rakovpublic.jneuropallium.worker.net.neuron.impl;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that NeuronRunnerService.process() blocks until all neuron processing
 * threads have finished – the key thread-synchronisation fix via CountDownLatch.
 */
class NeuronRunnerServiceTest {

    /**
     * Minimal INeuron stub that records how many times processSignals() was called
     * and simulates a small amount of work per neuron.
     */
    static class CountingNeuron extends Neuron {
        private final AtomicInteger processCount;

        CountingNeuron(AtomicInteger counter) {
            super();
            this.processCount = counter;
        }

        @Override
        public void processSignals() {
            processCount.incrementAndGet();
            // simulate a tiny bit of work
            try { Thread.sleep(5); } catch (InterruptedException ignored) {}
        }

        @Override
        public void activate() {
            isProcessed = true;
        }

        @Override
        public Boolean hasResult() {
            return isProcessed;
        }
    }

    @Test
    void testProcess_blocksUntilAllNeuronsProcessed() {
        // Use a fresh service (not the singleton) via a subclass hack to avoid
        // cross-test contamination; we actually use the singleton here but clear it.
        NeuronRunnerService svc = NeuronRunnerService.getService();
        // Drain any leftover queue from other tests
        while (svc.getNeuronQueue().poll() != null) { /* drain */ }

        AtomicInteger counter = new AtomicInteger(0);
        int neuronCount = 10;
        for (int i = 0; i < neuronCount; i++) {
            svc.addNeuron(new CountingNeuron(counter));
        }

        // process() should block until all threads finish
        svc.process(4);

        assertEquals(neuronCount, counter.get(),
                "All neurons must be processed before process() returns");
    }

    @Test
    void testProcess_emptyQueue_returnsImmediately() {
        NeuronRunnerService svc = NeuronRunnerService.getService();
        while (svc.getNeuronQueue().poll() != null) { /* drain */ }

        // Should not hang even with an empty queue
        assertDoesNotThrow(() -> svc.process(2));
    }

    @Test
    void testProcess_alreadyProcessedNeurons_skipped() {
        NeuronRunnerService svc = NeuronRunnerService.getService();
        while (svc.getNeuronQueue().poll() != null) { /* drain */ }

        AtomicInteger counter = new AtomicInteger(0);
        CountingNeuron n = new CountingNeuron(counter);
        n.isProcessed = true; // hasResult() returns true → NeuronRunner skips it
        svc.addNeuron(n);

        svc.process(1);

        assertEquals(0, counter.get(), "Already-processed neuron should not be reprocessed");
    }
}
