package web.neuron;

/**
 * Created by Rakovskyi Dmytro on 08.06.2018.
 */
public interface INeuronFactory {
    <K extends INeuron> K getNeuron(String json, Class<K> tClass);

}
