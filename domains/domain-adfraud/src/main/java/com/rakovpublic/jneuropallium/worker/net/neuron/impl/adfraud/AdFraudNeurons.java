/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.adfraud;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class EventAuthenticityNeuron extends AdFraudRuntimeScorer {
    EventAuthenticityNeuron() { super(); this.currentNeuronClass = EventAuthenticityNeuron.class; }
}

class HumanInteractionNeuron extends AdFraudRuntimeScorer {
    HumanInteractionNeuron() { super(); this.currentNeuronClass = HumanInteractionNeuron.class; }
}

class SessionSequenceNeuron extends AdFraudRuntimeScorer {
    SessionSequenceNeuron() { super(); this.currentNeuronClass = SessionSequenceNeuron.class; }
}

class AttributionIntegrityNeuron extends AdFraudRuntimeScorer {
    AttributionIntegrityNeuron() { super(); this.currentNeuronClass = AttributionIntegrityNeuron.class; }
}

class PublisherBaselineNeuron extends AdFraudRuntimeScorer {
    private final Map<String, Double> baselineRisk = new HashMap<>();
    private int frozenUpdates;

    PublisherBaselineNeuron() { super(); this.currentNeuronClass = PublisherBaselineNeuron.class; }

    void observe(String entity, double risk, boolean strongFraudEvidence) {
        if (strongFraudEvidence) {
            frozenUpdates++;
            return;
        }
        baselineRisk.merge(entity, risk, (oldValue, newValue) -> (0.90 * oldValue) + (0.10 * newValue));
    }

    Double baselineFor(String entity) { return baselineRisk.get(entity); }
    int getFrozenUpdates() { return frozenUpdates; }
}

class ClickFarmGraphNeuron extends AdFraudRuntimeScorer {
    private final Map<String, Set<String>> neighbours = new HashMap<>();
    private long ttlMs = 3_600_000L;

    ClickFarmGraphNeuron() { super(); this.currentNeuronClass = ClickFarmGraphNeuron.class; }

    void setGraphTtlMs(long ttlMs) { this.ttlMs = Math.max(1L, ttlMs); }

    double observeEdge(String left, String right, long nowMs) {
        neighbours.computeIfAbsent(left, ignored -> new HashSet<>()).add(right + "@" + (nowMs / ttlMs));
        return Math.min(1.0, neighbours.get(left).size() / 20.0);
    }

    void evict(long nowMs) {
        long bucket = nowMs / ttlMs;
        for (Set<String> set : neighbours.values()) {
            set.removeIf(value -> {
                int at = value.lastIndexOf('@');
                if (at < 0) return true;
                long valueBucket = Long.parseLong(value.substring(at + 1));
                return bucket - valueBucket > 1;
            });
        }
    }

    int degree(String left) { return neighbours.getOrDefault(left, Set.of()).size(); }
}

class TrafficQualityNeuron extends AdFraudRuntimeScorer {
    TrafficQualityNeuron() { super(); this.currentNeuronClass = TrafficQualityNeuron.class; }
}

// Behavioural-evidence layer (layer 3): single-purpose neurons that each emit
// one fraud-family-specific evidence feature, separating the conflated labels.
class ClickVolumeNeuron extends AdFraudRuntimeScorer {
    ClickVolumeNeuron() { super(); this.currentNeuronClass = ClickVolumeNeuron.class; }
}

class ConversionTimingNeuron extends AdFraudRuntimeScorer {
    ConversionTimingNeuron() { super(); this.currentNeuronClass = ConversionTimingNeuron.class; }
}

class IncentivePatternNeuron extends AdFraudRuntimeScorer {
    IncentivePatternNeuron() { super(); this.currentNeuronClass = IncentivePatternNeuron.class; }
}

class LowValueQualityNeuron extends AdFraudRuntimeScorer {
    LowValueQualityNeuron() { super(); this.currentNeuronClass = LowValueQualityNeuron.class; }
}

// Feature-interaction layer (layer 4): one neuron per non-linear hidden unit.
class FeatureInteractionNeuron extends AdFraudRuntimeScorer {
    FeatureInteractionNeuron() { super(); this.currentNeuronClass = FeatureInteractionNeuron.class; }
}

class FraudCorrelationNeuron extends AdFraudRuntimeScorer {
    FraudCorrelationNeuron() { super(); this.currentNeuronClass = FraudCorrelationNeuron.class; }
}

class FraudResponseGateNeuron extends AdFraudRuntimeScorer {
    private AdFraudRuntimeMode runtimeMode = AdFraudRuntimeMode.ADVISORY;

    FraudResponseGateNeuron() { super(); this.currentNeuronClass = FraudResponseGateNeuron.class; }

    void setRuntimeMode(AdFraudRuntimeMode runtimeMode) {
        this.runtimeMode = runtimeMode == null ? AdFraudRuntimeMode.ADVISORY : runtimeMode;
        setMode(this.runtimeMode);
    }

    boolean isAutomaticActionAllowed() {
        return false;
    }

    AdFraudRuntimeMode getRuntimeMode() { return runtimeMode; }
}
