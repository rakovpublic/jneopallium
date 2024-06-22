package com.rakovpublic.jneuropallium.worker.net.neuron.impl;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuronMeta;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/***
 * Created by Rakovskyi Dmytro on 14.06.2018.
 */
public class NeuronRunnerService {

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

        for (int i = 0; i < poolSize; i++) {
            NeuronRunner ne = new NeuronRunner(this);
            Thread th = new Thread(ne);
            th.start();
        }
    }

    public ConcurrentLinkedQueue<INeuron> getNeuronQueue() {
        return neuronQueue;
    }


    public static NeuronRunnerService getService() {
        return service;
    }
}
