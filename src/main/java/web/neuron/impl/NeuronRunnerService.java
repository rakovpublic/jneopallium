package web.neuron.impl;

import synchronizer.Context;
import web.neuron.INeuron;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Rakovskyi Dmytro on 14.06.2018.
 */
public class NeuronRunnerService {

    private List<INeuron> neuronQueue;
    private static NeuronRunnerService service= new NeuronRunnerService();

    private NeuronRunnerService() {

        neuronQueue=new LinkedList<>();


    }


    public void addNeuron(INeuron neuron){
        neuronQueue.add(neuron);
    }
    public void process(){
        for(INeuron neuron:neuronQueue){
            NeuronRunner ne=new NeuronRunner(this);
            ne.setNeuron(neuron);
            Thread th= new Thread(ne);
            th.start();
        }
    }

    public static NeuronRunnerService getService() {
        return service;
    }
}
