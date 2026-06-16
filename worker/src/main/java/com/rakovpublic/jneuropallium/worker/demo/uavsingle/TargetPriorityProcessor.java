package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TargetPriorityProcessor {
    public TargetPriority calculate(ObservationTarget target, UavPose pose, UavSingleConfig config,
                                    double duplicationPenalty) {
        double distance = pose.distance2d(target.x, target.y);
        Map<String, Double> factors = new LinkedHashMap<>();
        factors.put("missionRelevance", clamp(target.missionRelevance));
        factors.put("confidence", clamp(target.confidence));
        factors.put("urgency", clamp(target.urgency));
        factors.put("informationValue", clamp(target.informationValue));
        factors.put("proximityBenefit", clamp(1.0 - distance / 500.0));
        factors.put("routeEfficiency", clamp(1.0 - distance / 750.0));
        factors.put("communicationValue", clamp(target.communicationValue));
        factors.put("safetyRisk", clamp(target.safetyRisk));
        factors.put("energyCost", clamp(distance / 500.0));
        factors.put("duplicationPenalty", clamp(duplicationPenalty));
        double score = 0.0;
        for (Map.Entry<String, Double> factor : factors.entrySet()) {
            score += config.priorityWeights.getOrDefault(factor.getKey(), 0.0) * factor.getValue();
        }
        return new TargetPriority(target.targetId, factors, clamp(score));
    }

    public TargetPriority selectBest(List<TargetPriority> priorities) {
        return priorities.stream()
                .max(Comparator.comparingDouble((TargetPriority p) -> p.score)
                        .thenComparing(p -> p.targetId, Comparator.reverseOrder()))
                .orElseThrow(() -> new IllegalArgumentException("no target priorities to select"));
    }

    static double clamp(double value) {
        if (Double.isNaN(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }
}

