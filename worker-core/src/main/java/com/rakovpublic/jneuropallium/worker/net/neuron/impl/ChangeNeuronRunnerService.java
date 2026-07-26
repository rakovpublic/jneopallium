package com.rakovpublic.jneuropallium.worker.net.neuron.impl;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuronMeta;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

public class ChangeNeuronRunnerService {
    private static final Logger logger = LogManager.getLogger(ChangeNeuronRunnerService.class);

    private final ConcurrentLinkedQueue<INeuron> neuronQueue;
    private static final ChangeNeuronRunnerService service = new ChangeNeuronRunnerService();


    private ChangeNeuronRunnerService() {

        neuronQueue = new ConcurrentLinkedQueue<>();


    }


    public void addNeuron(INeuron neuron) {
        neuronQueue.add(neuron);
    }

    public void addNeurons(List<INeuronMeta<? extends INeuron>> metas) {
        for (INeuronMeta m : metas) {
            addNeuron(m.toNeuron());
        }
    }

    public void process(int poolSize) {
        CountDownLatch latch = new CountDownLatch(poolSize);
        for (int i = 0; i < poolSize; i++) {
            NeuronChangeRunner ne = new NeuronChangeRunner(this, latch);
            Thread th = new Thread(ne);
            th.start();
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("ChangeNeuronRunnerService interrupted while waiting for neurons to process", e);
        }
    }

    public ConcurrentLinkedQueue<INeuron> getNeuronQueue() {
        return neuronQueue;
    }


    public static ChangeNeuronRunnerService getService() {
        return service;
    }
}
