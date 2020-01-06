package net.study;

import net.neuron.INConnection;
import net.neuron.IWeight;
import net.signals.ISignal;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
/**
 * Request for rebuilding neuron connections
 * */
public interface IStudyingRequest extends Serializable {
    int getLayerId();

    Long getNeuronId();

    HashMap<Class<? extends ISignal>, List<INConnection>> getNewConnections();

    String toJSON();
}
