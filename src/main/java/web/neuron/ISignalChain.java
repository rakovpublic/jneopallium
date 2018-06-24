package web.neuron;

import web.signals.ISignal;

import java.util.List;

public interface ISignalChain {
    List<Class<? extends ISignal>> getProcessingChain();
}
