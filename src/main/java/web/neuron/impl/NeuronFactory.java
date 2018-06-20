package web.neuron.impl;

import exceptions.NeuronDeserializationException;
import web.neuron.INeuron;
import web.neuron.INeuronFactory;
import web.storages.INeuronSerializer;
import web.storages.IStorageMeta;

import java.util.HashMap;

/**
 * Created by Rakovskyi Dmytro on 13.06.2018.
 */
public class NeuronFactory implements INeuronFactory {
    private static NeuronFactory nf= new NeuronFactory();
    private HashMap<String,INeuronSerializer> map;

    private NeuronFactory() {
        map=new HashMap<String,INeuronSerializer>();
    }
    public static NeuronFactory getNeuronFactory(){
        return nf;
    }

    @Override
    public <K extends INeuron,J extends IStorageMeta> K getNeuron(J json, Class<K> tClass) {
        if(!map.containsKey(tClass.getSimpleName())){
            throw new NeuronDeserializationException();
        }
        INeuronSerializer des=map.get(tClass.getSimpleName());
        return (K)des.deserialize(json);
    }

    @Override
    public <K extends INeuron, J extends IStorageMeta> void registerNeuronClass(Class<K> tClass, INeuronSerializer<J, K> serializer) {
        map.put(tClass.getSimpleName(),serializer);

    }

}
