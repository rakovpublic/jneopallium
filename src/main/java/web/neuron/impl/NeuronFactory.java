package web.neuron.impl;

import exceptions.NeuronDeserializationException;
import web.neuron.INeuron;
import web.neuron.INeuronDeserializer;
import web.neuron.INeuronFactory;
import web.storages.INeuronSerializer;

import java.util.HashMap;

/**
 * Created by Rakovskyi Dmytro on 13.06.2018.
 */
public class NeuronFactory implements INeuronFactory {
    private static NeuronFactory nf= new NeuronFactory();
    private HashMap<Class<INeuron>,INeuronSerializer> map;

    private NeuronFactory() {
        map=new HashMap<Class<INeuron>,INeuronSerializer>();
    }
    public static NeuronFactory getNeuronFactory(){
        return nf;
    }

    @Override
    public <K extends INeuron,J> K getNeuron(J json, Class<K> tClass) {
        if(!map.containsKey(tClass)){
            throw new NeuronDeserializationException();
        }
        INeuronSerializer des=map.get(tClass);
        return (K)des.deserialize(json);
    }
}
