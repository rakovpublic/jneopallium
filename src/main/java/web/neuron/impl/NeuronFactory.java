package web.neuron.impl;

import exceptions.NeuronDeserializationException;
import web.neuron.INeuron;
import web.neuron.INeuronDeserializer;
import web.neuron.INeuronFactory;

import java.util.HashMap;

/**
 * Created by Rakovskyi Dmytro on 13.06.2018.
 */
public class NeuronFactory implements INeuronFactory {
    private static NeuronFactory nf= new NeuronFactory();
    private HashMap<Class<INeuron>,INeuronDeserializer> map;

    private NeuronFactory() {
        map=new HashMap<Class<INeuron>,INeuronDeserializer>();
    }
    public static NeuronFactory getNeuronFactory(){
        return nf;
    }

    @Override
    public <K extends INeuron> K getNeuron(String json, Class<K> tClass) {
        if(!map.containsKey(tClass)){
            throw new NeuronDeserializationException();
        }
        INeuronDeserializer des=map.get(tClass);
        return (K)des.toNeuron(json);
    }
}
