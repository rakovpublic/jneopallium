package com.rakovpublic.jneuropallium.worker.net.neuron.impl;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

/***
 * Created by Rakovskyi Dmytro on 14.06.2018.
 */
class NeuronRunner implements Runnable {
    private final NeuronRunnerService service;
    private final CountDownLatch latch;


    NeuronRunner(NeuronRunnerService service, CountDownLatch latch) {
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
                    neuron.processSignals();
                    neuron.activate();
                }
            }
        } finally {
            latch.countDown();
        }
    }
}
