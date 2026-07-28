package com.rakovpublic.jneuropallium.worker.demo.fullrun.demo07.processors;

import com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoSignalProcessor;

public class ObservabilityOtelProcessor extends DemoSignalProcessor {
    public ObservabilityOtelProcessor() {
        signalProcessorClass = ObservabilityOtelProcessor.class.getName();
    }
}
