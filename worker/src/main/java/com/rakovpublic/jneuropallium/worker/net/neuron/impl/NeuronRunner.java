package com.rakovpublic.jneuropallium.worker.net.neuron.impl;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;

import java.util.concurrent.ConcurrentLinkedQueue;

/***
 * Created by Rakovskyi Dmytro on 14.06.2018.
 */
class NeuronRunner implements Runnable {
    private final NeuronRunnerService service;


    NeuronRunner(NeuronRunnerService service) {
        this.service = service;

    }

    @Override
    public void run() {
        ConcurrentLinkedQueue<INeuron> queue = service.getNeuronQueue();
        INeuron neuron;
        while ((neuron = queue.poll()) != null) {
            if (!neuron.hasResult()) {
                neuron.processSignals();
                neuron.activate();
            }
        }
    }
}
