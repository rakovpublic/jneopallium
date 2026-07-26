package com.rakovpublic.jneuropallium.worker.demo.fullrun.demo05.processors;

import com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoSignalProcessor;

public class DicomContextProcessor extends DemoSignalProcessor {
    public DicomContextProcessor() {
        signalProcessorClass = DicomContextProcessor.class.getName();
    }
}
