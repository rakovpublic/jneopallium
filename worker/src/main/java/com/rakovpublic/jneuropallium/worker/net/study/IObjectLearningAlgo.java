package com.rakovpublic.jneuropallium.worker.net.study;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.io.Serializable;
import java.util.List;

public interface IObjectLearningAlgo extends ILearningAlgo, Serializable {
    /**
     * this method return signals for study
     */
    List<ISignal> getLearningSignals();
}
