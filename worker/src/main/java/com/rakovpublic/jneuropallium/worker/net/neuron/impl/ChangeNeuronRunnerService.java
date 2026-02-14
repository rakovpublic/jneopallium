package com.rakovpublic.jneuropallium.worker.net.neuron.impl;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuronMeta;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ChangeNeuronRunnerService {
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

        for (int i = 0; i < poolSize; i++) {
            NeuronChangeRunner ne = new NeuronChangeRunner(this);
            Thread th = new Thread(ne);
            th.start();
        }
    }

    public ConcurrentLinkedQueue<INeuron> getNeuronQueue() {
        return neuronQueue;
    }


    public static ChangeNeuronRunnerService getService() {
        return service;
    }
}

