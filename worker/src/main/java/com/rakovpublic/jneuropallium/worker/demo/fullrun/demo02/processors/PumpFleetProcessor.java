package com.rakovpublic.jneuropallium.worker.demo.fullrun.demo02.processors;

import com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoSignalProcessor;

public class PumpFleetProcessor extends DemoSignalProcessor {
    public PumpFleetProcessor() {
        signalProcessorClass = PumpFleetProcessor.class.getName();
    }
}
