package web.neuron.impl;

import web.neuron.INeuron;

import java.util.Queue;

/**
 * Created by Rakovskyi Dmytro on 14.06.2018.
 */
class NeuronRunner implements Runnable {
    private NeuronRunnerService service;


    NeuronRunner(NeuronRunnerService service) {
        this.service = service;

    }

    @Override
    public void run() {
        Queue<INeuron> queue = service.getNeuronQueue();
        INeuron neuron;
        while ((neuron = queue.poll()) != null) {
            neuron.processSignals();
        }
    }
}
