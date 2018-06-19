package web.neuron;

import web.storages.INeuronSerializer;

/**
 * Created by Rakovskyi Dmytro on 08.06.2018.
 */
public interface INeuronFactory {
    <K extends INeuron,J> K getNeuron(J json, Class<K> tClass);
    <K extends INeuron,J>void registerNeuronClass(Class<K> tClass, INeuronSerializer<J,K> serializer);

}
