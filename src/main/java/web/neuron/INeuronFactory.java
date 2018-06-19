package web.neuron;

/**
 * Created by Rakovskyi Dmytro on 08.06.2018.
 */
public interface INeuronFactory {
    <K extends INeuron,J> K getNeuron(J json, Class<K> tClass);

}
