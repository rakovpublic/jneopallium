package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime;

import com.rakovpublic.jneuropallium.worker.application.IOutputAggregator;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.util.IContext;

import java.util.List;

public class AutonomousMindResultAggregator implements IOutputAggregator {
    @Override
    public void save(List<IResult> results, long timestamp, long run, IContext context) {
        AutonomousMindSimulation.advance(context);
    }
}
