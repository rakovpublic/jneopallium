package com.rakovpublic.jneuropallium.worker.demo.autonomousai.runtime;

import com.rakovpublic.jneuropallium.worker.application.IOutputAggregator;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.util.IContext;

import java.util.List;

public class AutonomousAiResultAggregator implements IOutputAggregator {
    @Override
    public void save(List<IResult> results, long timestamp, long run, IContext context) {
        AutonomousAiSimulation.advance(context);
    }
}
