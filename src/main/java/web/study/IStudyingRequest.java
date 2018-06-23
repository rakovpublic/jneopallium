package web.study;

import web.neuron.INConnection;
import web.neuron.IWeight;
import web.signals.ISignal;

import java.util.HashMap;

public interface IStudyingRequest {
    int getLayerId();
    String getNeuronId();
    HashMap<Class<? extends ISignal>,HashMap<INConnection,IWeight>> getNewConnections();
    String toJSON();
}
