/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.adfraud;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.adfraud.AdFraudEvent;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;

class AdBidSignal extends AdFraudSignal {
    static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);
    AdBidSignal() { super(AdBidSignal.class, PROCESSING_FREQUENCY, null); }
    AdBidSignal(AdFraudEvent e) { super(AdBidSignal.class, PROCESSING_FREQUENCY, e); }
}

class AdImpressionSignal extends AdFraudSignal {
    static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);
    AdImpressionSignal() { super(AdImpressionSignal.class, PROCESSING_FREQUENCY, null); }
    AdImpressionSignal(AdFraudEvent e) { super(AdImpressionSignal.class, PROCESSING_FREQUENCY, e); }
}

class AdClickSignal extends AdFraudSignal {
    static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);
    AdClickSignal() { super(AdClickSignal.class, PROCESSING_FREQUENCY, null); }
    AdClickSignal(AdFraudEvent e) { super(AdClickSignal.class, PROCESSING_FREQUENCY, e); }
}

class LandingSignal extends AdFraudSignal {
    static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);
    LandingSignal() { super(LandingSignal.class, PROCESSING_FREQUENCY, null); }
    LandingSignal(AdFraudEvent e) { super(LandingSignal.class, PROCESSING_FREQUENCY, e); }
}

class UserInteractionSignal extends AdFraudSignal {
    static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(2L, 1);
    UserInteractionSignal() { super(UserInteractionSignal.class, PROCESSING_FREQUENCY, null); }
    UserInteractionSignal(AdFraudEvent e) { super(UserInteractionSignal.class, PROCESSING_FREQUENCY, e); }
}

class ConversionSignal extends AdFraudSignal {
    static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(5L, 1);
    ConversionSignal() { super(ConversionSignal.class, PROCESSING_FREQUENCY, null); }
    ConversionSignal(AdFraudEvent e) { super(ConversionSignal.class, PROCESSING_FREQUENCY, e); }
}

class PostbackSignal extends AdFraudSignal {
    static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);
    PostbackSignal() { super(PostbackSignal.class, PROCESSING_FREQUENCY, null); }
    PostbackSignal(AdFraudEvent e) { super(PostbackSignal.class, PROCESSING_FREQUENCY, e); }
}

class RetentionSignal extends AdFraudSignal {
    static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(10L, 2);
    RetentionSignal() { super(RetentionSignal.class, PROCESSING_FREQUENCY, null); }
    RetentionSignal(AdFraudEvent e) { super(RetentionSignal.class, PROCESSING_FREQUENCY, e); }
}

class RefundSignal extends AdFraudSignal {
    static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(10L, 2);
    RefundSignal() { super(RefundSignal.class, PROCESSING_FREQUENCY, null); }
    RefundSignal(AdFraudEvent e) { super(RefundSignal.class, PROCESSING_FREQUENCY, e); }
}

class BillingSignal extends AdFraudSignal {
    static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(10L, 2);
    BillingSignal() { super(BillingSignal.class, PROCESSING_FREQUENCY, null); }
    BillingSignal(AdFraudEvent e) { super(BillingSignal.class, PROCESSING_FREQUENCY, e); }
}

class DeviceAttestationSignal extends AdFraudSignal {
    static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);
    DeviceAttestationSignal() { super(DeviceAttestationSignal.class, PROCESSING_FREQUENCY, null); }
    DeviceAttestationSignal(AdFraudEvent e) { super(DeviceAttestationSignal.class, PROCESSING_FREQUENCY, e); }
}

class SupplyChainSignal extends AdFraudSignal {
    static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 2);
    SupplyChainSignal() { super(SupplyChainSignal.class, PROCESSING_FREQUENCY, null); }
    SupplyChainSignal(AdFraudEvent e) { super(SupplyChainSignal.class, PROCESSING_FREQUENCY, e); }
}

class EventIntegritySignal extends AdFraudSignal {
    static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);
    EventIntegritySignal() { super(EventIntegritySignal.class, PROCESSING_FREQUENCY, null); }
    EventIntegritySignal(AdFraudEvent e) { super(EventIntegritySignal.class, PROCESSING_FREQUENCY, e); }
}

class SessionBehaviourSignal extends AdFraudSignal {
    static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(5L, 1);
    SessionBehaviourSignal() { super(SessionBehaviourSignal.class, PROCESSING_FREQUENCY, null); }
    SessionBehaviourSignal(AdFraudEvent e) { super(SessionBehaviourSignal.class, PROCESSING_FREQUENCY, e); }
}

class AttributionAnomalySignal extends AdFraudSignal {
    static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(5L, 1);
    AttributionAnomalySignal() { super(AttributionAnomalySignal.class, PROCESSING_FREQUENCY, null); }
    AttributionAnomalySignal(AdFraudEvent e) { super(AttributionAnomalySignal.class, PROCESSING_FREQUENCY, e); }
}

class EntityBaselineSignal extends AdFraudSignal {
    static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 2);
    EntityBaselineSignal() { super(EntityBaselineSignal.class, PROCESSING_FREQUENCY, null); }
    EntityBaselineSignal(AdFraudEvent e) { super(EntityBaselineSignal.class, PROCESSING_FREQUENCY, e); }
}

class GraphClusterSignal extends AdFraudSignal {
    static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(3L, 2);
    GraphClusterSignal() { super(GraphClusterSignal.class, PROCESSING_FREQUENCY, null); }
    GraphClusterSignal(AdFraudEvent e) { super(GraphClusterSignal.class, PROCESSING_FREQUENCY, e); }
}

class TrafficQualitySignal extends AdFraudSignal {
    static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(5L, 2);
    TrafficQualitySignal() { super(TrafficQualitySignal.class, PROCESSING_FREQUENCY, null); }
    TrafficQualitySignal(AdFraudEvent e) { super(TrafficQualitySignal.class, PROCESSING_FREQUENCY, e); }
}

class FraudEvidenceSignal extends AdFraudSignal {
    static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);
    FraudEvidenceSignal() { super(FraudEvidenceSignal.class, PROCESSING_FREQUENCY, null); }
    FraudEvidenceSignal(AdFraudEvent e) { super(FraudEvidenceSignal.class, PROCESSING_FREQUENCY, e); }
}

class FraudHypothesisSignal extends AdFraudSignal {
    static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 2);
    FraudHypothesisSignal() { super(FraudHypothesisSignal.class, PROCESSING_FREQUENCY, null); }
    FraudHypothesisSignal(AdFraudEvent e) { super(FraudHypothesisSignal.class, PROCESSING_FREQUENCY, e); }
}

class FraudDecisionSignal extends AdFraudSignal {
    static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);
    FraudDecisionSignal() { super(FraudDecisionSignal.class, PROCESSING_FREQUENCY, null); }
    FraudDecisionSignal(AdFraudEvent e) { super(FraudDecisionSignal.class, PROCESSING_FREQUENCY, e); }
}

class ModelDriftSignal extends AdFraudSignal {
    static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(60L, 2);
    ModelDriftSignal() { super(ModelDriftSignal.class, PROCESSING_FREQUENCY, null); }
    ModelDriftSignal(AdFraudEvent e) { super(ModelDriftSignal.class, PROCESSING_FREQUENCY, e); }
}

class AnalystFeedbackSignal extends AdFraudSignal {
    static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(10L, 2);
    AnalystFeedbackSignal() { super(AnalystFeedbackSignal.class, PROCESSING_FREQUENCY, null); }
    AnalystFeedbackSignal(AdFraudEvent e) { super(AnalystFeedbackSignal.class, PROCESSING_FREQUENCY, e); }
}
