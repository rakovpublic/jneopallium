package com.rakovpublic.jneuropallium.worker.net.neuron.impl.sleep;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.sleep.ReplaySignal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public interface IHippocampalReplayNeuron extends IModulatableNeuron {
    void recordEpisode(String id, List<Long> neuronSequence, double salience);
    List<ReplaySignal> emitTopK();
    int bufferedEpisodes();
    int getTopK();
    void setTopK(int v);
    double getCompressionRatio();
    void setCompressionRatio(double v);
    ReplayDirection getDirection();
    void setDirection(ReplayDirection d);
    Map<String, Double> debugSalienceMap();
}
