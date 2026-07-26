package com.rakovpublic.jneuropallium.worker.net.study;

import com.rakovpublic.jneuropallium.worker.net.layers.IStructMeta;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public interface IObjectLearningAlgo extends ILearningAlgo, Serializable {
    /**
     * this method return signals for study
     */
    CopyOnWriteArrayList<ISignal> getLearningSignals(HashMap<String, List<IResultSignal>> expected, IStructMeta meta);
}
