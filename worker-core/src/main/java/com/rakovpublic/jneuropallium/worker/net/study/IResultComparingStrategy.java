package com.rakovpublic.jneuropallium.worker.net.study;

import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;

import java.util.HashMap;
import java.util.List;

public interface IResultComparingStrategy {
    List<IResult> getIdsStudy(List<IResult> resultLayer, HashMap<String, List<IResultSignal>> desiredResult);
}
