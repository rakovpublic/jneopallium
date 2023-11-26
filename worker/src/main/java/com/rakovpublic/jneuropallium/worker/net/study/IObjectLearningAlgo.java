package com.rakovpublic.jneuropallium.worker.net.study;

import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.layers.IStructMeta;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

public interface IObjectLearningAlgo extends ILearningAlgo, Serializable {
    /**
     * this method return signals for study
     */
    List<ISignal> getLearningSignals(HashMap<String, List<IResultSignal>> expected, IStructMeta meta);
}
