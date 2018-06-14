package web.neuron;

/**
 * Created by Rakovskyi Dmytro on 02.11.2017.
 */
public interface INeuronDeserializer<N extends INeuron> {
    N toNeuron(String json);
}
