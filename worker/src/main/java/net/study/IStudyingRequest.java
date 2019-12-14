package net.study;

import net.neuron.INConnection;
import net.neuron.IWeight;
import net.signals.ISignal;

import java.io.Serializable;
import java.util.HashMap;

public interface IStudyingRequest extends Serializable {
    int getLayerId();

    String getNeuronId();

    HashMap<Class<? extends ISignal>, HashMap<INConnection, IWeight>> getNewConnections();

    String toJSON();
}
