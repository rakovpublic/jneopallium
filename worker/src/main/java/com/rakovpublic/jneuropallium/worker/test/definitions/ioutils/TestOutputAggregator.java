/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions.ioutils;

import com.rakovpublic.jneuropallium.worker.application.IOutputAggregator;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.util.IContext;

import java.util.List;

public class TestOutputAggregator implements IOutputAggregator {
    @Override
    public void save(List<IResult> results, long timestamp, long run, IContext context) {


    }
}
