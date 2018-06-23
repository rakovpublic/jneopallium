package web.neuron;

/**
 * Created by Rakovskyi Dmytro on 02.11.2017.
 */
public interface INConnection {
    int getTargetLayerId();
    int getSourceLayerId();
    String getTargetNeuronId();
    String getSourceNeuronId();
    String toJSON();
}
