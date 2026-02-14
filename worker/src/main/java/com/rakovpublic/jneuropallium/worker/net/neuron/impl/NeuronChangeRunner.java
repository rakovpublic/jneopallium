package com.rakovpublic.jneuropallium.worker.net.neuron.impl;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;

import java.util.concurrent.ConcurrentLinkedQueue;

public class NeuronChangeRunner implements Runnable {
    private final ChangeNeuronRunnerService service;


    NeuronChangeRunner(ChangeNeuronRunnerService service) {
        this.service = service;

    }
    @Override
    public void run() {
        ConcurrentLinkedQueue<INeuron> queue = service.getNeuronQueue();
        INeuron neuron;
        while ((neuron = queue.poll()) != null) {
            if (!neuron.hasResult()) {
                neuron.processWeightSignals();
            }
        }
    }
}
