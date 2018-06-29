package web.neuron;

/**
 * Created by Rakovskyi Dmytro on 02.11.2017.
 */
public interface INConnection {
    int getTargetLayerId();

    int getSourceLayerId();

    Long getTargetNeuronId();

    Long getSourceNeuronId();

    String toJSON();

    String getDescription();
}
