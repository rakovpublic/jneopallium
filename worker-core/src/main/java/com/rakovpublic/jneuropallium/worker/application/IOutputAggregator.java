package com.rakovpublic.jneuropallium.worker.application;

import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.util.IContext;

import java.util.List;

public interface IOutputAggregator {
    void save(List<IResult> results, long timestamp, long run, IContext context);
}
