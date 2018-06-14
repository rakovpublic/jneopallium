package web.neuron;

import web.signals.*;
import web.signals.ISignalProcessor;

import java.util.HashMap;
import java.util.List;

/**
 * Created by Rakovskyi Dmytro on 27.10.2017.
 */
public interface INeuron {
    void addSignals(List<ISignal> signals);
    void processSignals();
    void reconfigure();
    void setAxon(IAxon axon);
    <S extends ISignal> void addSignalProcessor(Class<S> clazz, ISignalProcessor processor);
    HashMap<ISignal,List<INConnection>> getResult();
    Boolean hasResult();
    String getId();
    String toJSON();

}
