package com.rakovpublic.jneuropallium.worker.demo.cluster;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoNeuron;
import com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.List;

/**
 * The arithmetic of the cluster demo: each stage scales the incoming value and stamps the stage
 * it came from, and the result stage turns the accumulated value into a decision.
 * <p>
 * Deliberately trivial and self contained - this demo is about how work is distributed, so the
 * model must stay cheap, deterministic and free of any scenario catalogue.
 */
public class ClusterSignalProcessor implements ISignalProcessor<DemoSignal, DemoNeuron> {
    private static final double ADVISORY_THRESHOLD = 100.0;

    public String signalProcessorClass = ClusterSignalProcessor.class.getName();
    public String signalClassName = DemoSignal.class.getName();
    public String stage = "stage";
    public boolean resultStage = false;

    public ClusterSignalProcessor() {
    }

    public ClusterSignalProcessor(String stage, boolean resultStage) {
        this.stage = stage;
        this.resultStage = resultStage;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(DemoSignal input, DemoNeuron neuron) {
        DemoSignal output = input.copySignal();
        output.setSignalType(stage);
        output.setNumericValue(input.getNumericValue() * 1.5);
        output.withAttribute("stage", stage);
        output.withAttribute("neuron", neuron.getId());
        if (resultStage) {
            boolean advisory = output.getNumericValue() >= ADVISORY_THRESHOLD;
            output.setResultType(advisory ? "ADVISORY" : "NOMINAL");
            output.setDecision(advisory ? "REVIEW" : "PASS");
            output.setReason("aggregate " + String.format(java.util.Locale.ROOT, "%.2f", output.getNumericValue())
                    + (advisory ? " above " : " below ") + ADVISORY_THRESHOLD);
        }
        return (List<I>) List.of(output);
    }

    @Override
    public String getDescription() {
        return "cluster demo " + stage;
    }

    @Override
    public Boolean hasMerger() {
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends ISignalProcessor> getSignalProcessorClass() {
        try {
            return (Class<? extends ISignalProcessor>) Class.forName(signalProcessorClass);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Cannot resolve processor class " + signalProcessorClass, e);
        }
    }

    @Override
    @JsonIgnore
    public Class<DemoNeuron> getNeuronClass() {
        return DemoNeuron.class;
    }

    @Override
    @JsonIgnore
    @SuppressWarnings("unchecked")
    public Class<DemoSignal> getSignalClass() {
        try {
            return (Class<DemoSignal>) Class.forName(signalClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Cannot resolve signal class " + signalClassName, e);
        }
    }
}
