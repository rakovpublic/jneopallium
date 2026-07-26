/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.adfraud;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.adfraud.AdFraudDecision;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.adfraud.IAdFraudScoringNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.adfraud.AdFraudSignal;

import java.util.LinkedList;
import java.util.List;

abstract class AbstractAdFraudProcessor implements ISignalProcessor<AdFraudSignal, IAdFraudScoringNeuron> {
    private final String description;

    AbstractAdFraudProcessor(String description) {
        this.description = description;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(AdFraudSignal input, IAdFraudScoringNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null || input.getEvent() == null) return out;
        AdFraudDecision decision = neuron.score(input.getEvent());
        AdFraudSignal result = new AdFraudSignal(input.getEvent());
        result.setDecision(decision);
        out.add((I) result);
        return out;
    }

    @Override public String getDescription() { return description; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<IAdFraudScoringNeuron> getNeuronClass() { return IAdFraudScoringNeuron.class; }
    @Override public Class<AdFraudSignal> getSignalClass() { return AdFraudSignal.class; }
}

class EventAuthenticityProcessor extends AbstractAdFraudProcessor {
    EventAuthenticityProcessor() { super("Advertising event authenticity and replay rules"); }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return EventAuthenticityProcessor.class; }
}

class HumanInteractionProcessor extends AbstractAdFraudProcessor {
    HumanInteractionProcessor() { super("Human interaction and automation evidence"); }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return HumanInteractionProcessor.class; }
}

class SessionSequenceProcessor extends AbstractAdFraudProcessor {
    SessionSequenceProcessor() { super("Ad session causal sequence evidence"); }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return SessionSequenceProcessor.class; }
}

class AttributionIntegrityProcessor extends AbstractAdFraudProcessor {
    AttributionIntegrityProcessor() { super("Click spam, click injection and attribution integrity evidence"); }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return AttributionIntegrityProcessor.class; }
}

class PublisherBaselineProcessor extends AbstractAdFraudProcessor {
    PublisherBaselineProcessor() { super("Time-aware publisher and campaign baseline evidence"); }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return PublisherBaselineProcessor.class; }
}

class ClickFarmGraphProcessor extends AbstractAdFraudProcessor {
    ClickFarmGraphProcessor() { super("Rolling heterogeneous graph and click-farm evidence"); }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return ClickFarmGraphProcessor.class; }
}

class TrafficQualityProcessor extends AbstractAdFraudProcessor {
    TrafficQualityProcessor() { super("Delayed retention, refund and traffic-quality evidence"); }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return TrafficQualityProcessor.class; }
}

class FraudCorrelationProcessor extends AbstractAdFraudProcessor {
    FraudCorrelationProcessor() { super("Calibrated multi-label fraud probability fusion"); }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return FraudCorrelationProcessor.class; }
}

class FraudResponseGateProcessor extends AbstractAdFraudProcessor {
    FraudResponseGateProcessor() { super("Advisory-only response gate for candidate actions"); }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return FraudResponseGateProcessor.class; }
}
