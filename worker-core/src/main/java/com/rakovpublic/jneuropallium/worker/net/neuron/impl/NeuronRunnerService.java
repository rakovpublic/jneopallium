package com.rakovpublic.jneuropallium.worker.net.neuron.impl;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuronMeta;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

/***
 * Created by Rakovskyi Dmytro on 14.06.2018.
 */
public class NeuronRunnerService {
    private static final Logger logger = LogManager.getLogger(NeuronRunnerService.class);

    private final ConcurrentLinkedQueue<INeuron> neuronQueue;
    private static final NeuronRunnerService service = new NeuronRunnerService();


    private NeuronRunnerService() {

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
            NeuronRunner ne = new NeuronRunner(this, latch);
            Thread th = new Thread(ne);
            th.start();
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("NeuronRunnerService interrupted while waiting for neurons to process", e);
        }
    }

    public ConcurrentLinkedQueue<INeuron> getNeuronQueue() {
        return neuronQueue;
    }


    public static NeuronRunnerService getService() {
        return service;
    }
}
