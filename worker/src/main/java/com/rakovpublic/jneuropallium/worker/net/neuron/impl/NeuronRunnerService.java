package com.rakovpublic.jneuropallium.worker.net.neuron.impl;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuronMeta;
import com.rakovpublic.jneuropallium.worker.util.Context;
import com.rakovpublic.jneuropallium.worker.util.IContext;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

/***
 * Created by Rakovskyi Dmytro on 14.06.2018.
 */
public class NeuronRunnerService {

    private ConcurrentLinkedQueue<INeuron> neuronQueue;
    private static NeuronRunnerService service = new NeuronRunnerService();
    private static String neuronPool = "neuron.pool.size";
    private IContext context;

    private NeuronRunnerService() {

        neuronQueue = new ConcurrentLinkedQueue<>();
        context = Context.getContext();


    }


    public void addNeuron(INeuron neuron) {
        neuronQueue.add(neuron);
    }

    public void addNeurons(List<INeuronMeta<? extends INeuron>> metas) {
        for (INeuronMeta m : metas) {
            addNeuron(m.toNeuron());
        }
    }

    public void process() {
        int poolSize = Integer.parseInt(context.getProperty(neuronPool));
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
