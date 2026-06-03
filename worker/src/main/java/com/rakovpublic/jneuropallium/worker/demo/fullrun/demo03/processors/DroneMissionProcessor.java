package com.rakovpublic.jneuropallium.worker.demo.fullrun.demo03.processors;

import com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoSignalProcessor;

public class DroneMissionProcessor extends DemoSignalProcessor {
    public DroneMissionProcessor() {
        signalProcessorClass = DroneMissionProcessor.class.getName();
    }
}
