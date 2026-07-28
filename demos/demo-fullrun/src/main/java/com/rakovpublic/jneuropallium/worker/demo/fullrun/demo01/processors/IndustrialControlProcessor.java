package com.rakovpublic.jneuropallium.worker.demo.fullrun.demo01.processors;

import com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoSignalProcessor;

public class IndustrialControlProcessor extends DemoSignalProcessor {
    public IndustrialControlProcessor() {
        signalProcessorClass = IndustrialControlProcessor.class.getName();
    }
}
