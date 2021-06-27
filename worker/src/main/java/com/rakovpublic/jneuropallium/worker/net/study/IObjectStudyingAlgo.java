package com.rakovpublic.jneuropallium.worker.net.study;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

public interface IObjectStudyingAlgo extends IStudyingAlgo, Serializable {
    HashMap<Integer, HashMap<Long, List<ISignal>>> study(Long neuronId);
}
