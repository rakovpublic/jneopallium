package web.neuron;

import web.signals.ISignal;

import java.util.HashMap;
import java.util.List;

/**
 * Created by Rakovskyi Dmytro on 27.10.2017.
 */
public interface IAxon {
    HashMap<Class<ISignal>,HashMap<INConnection,IWeight>> getConnectedNeurons();
    String toJSON();
}
