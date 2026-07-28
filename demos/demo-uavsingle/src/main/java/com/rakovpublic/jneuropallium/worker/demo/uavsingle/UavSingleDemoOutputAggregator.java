package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import com.rakovpublic.jneuropallium.worker.application.IOutputAggregator;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.util.IContext;

import java.util.List;

public class UavSingleDemoOutputAggregator implements IOutputAggregator {
    @Override
    public void save(List<IResult> results, long timestamp, long run, IContext context) {
        // The deterministic UAV demo writes artifacts from UavSingleSimulation.
        // This class exists so the package can be wired into Entry local later
        // without inventing a parallel output surface.
    }
}

