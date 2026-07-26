package com.rakovpublic.jneuropallium.worker.demo.fullrun.demo06.processors;

import com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoSignalProcessor;

public class SecurityTriageProcessor extends DemoSignalProcessor {
    public SecurityTriageProcessor() {
        signalProcessorClass = SecurityTriageProcessor.class.getName();
    }
}
