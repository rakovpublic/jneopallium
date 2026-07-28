package com.rakovpublic.jneuropallium.worker.net.neuron.impl;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

public class NeuronChangeRunner implements Runnable {
    private final ChangeNeuronRunnerService service;
    private final CountDownLatch latch;


    NeuronChangeRunner(ChangeNeuronRunnerService service, CountDownLatch latch) {
        this.service = service;
        this.latch = latch;
    }

    @Override
    public void run() {
        try {
            ConcurrentLinkedQueue<INeuron> queue = service.getNeuronQueue();
            INeuron neuron;
            while ((neuron = queue.poll()) != null) {
                if (!neuron.hasResult()) {
                    neuron.processWeightSignals();
                }
            }
        } finally {
            latch.countDown();
        }
    }
}
