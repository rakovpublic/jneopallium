package web.neuron.impl;

import web.neuron.INeuron;

import java.util.Queue;

/**
 * Created by Rakovskyi Dmytro on 14.06.2018.
 */
 class NeuronRunner implements Runnable{
    private NeuronRunnerService service;
    private INeuron neuron;


    NeuronRunner(NeuronRunnerService service) {
        this.service = service;

    }

    public void setNeuron(INeuron neuron) {
        this.neuron = neuron;
    }

    @Override
    public void run() {





    }
}
