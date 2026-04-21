package com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.Neuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;
import java.util.HashMap;

public interface ICycleNeuron extends INeuron {
    void setSignalProcessingFrequencyMap(HashMap<Class<? extends ISignal>, ProcessingFrequency> signalProcessingFrequencyMap);
    void setInputProcessingFrequencyHashMap(HashMap<IInitInput, ProcessingFrequency> inputProcessingFrequencyHashMap);
    int getLoopCount();
    void setLoopCount(int loopCount);
    HashMap<IInitInput, ProcessingFrequency> getInputProcessingFrequencyHashMap();
    HashMap<Class<? extends ISignal>, ProcessingFrequency> getSignalProcessingFrequencyMap();
    void activate();
}
