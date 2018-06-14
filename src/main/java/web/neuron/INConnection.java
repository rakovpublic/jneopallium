package web.neuron;

/**
 * Created by Rakovskyi Dmytro on 02.11.2017.
 */
public interface INConnection {
    String getLayerId();
    String getNeuronId();
    String toJSON();
}
