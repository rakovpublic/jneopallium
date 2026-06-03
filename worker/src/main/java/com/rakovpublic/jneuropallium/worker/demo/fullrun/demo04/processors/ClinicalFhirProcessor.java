package com.rakovpublic.jneuropallium.worker.demo.fullrun.demo04.processors;

import com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoSignalProcessor;

public class ClinicalFhirProcessor extends DemoSignalProcessor {
    public ClinicalFhirProcessor() {
        signalProcessorClass = ClinicalFhirProcessor.class.getName();
    }
}
