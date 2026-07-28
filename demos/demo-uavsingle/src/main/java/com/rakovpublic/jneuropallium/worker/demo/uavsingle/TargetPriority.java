package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import java.util.LinkedHashMap;
import java.util.Map;

public class TargetPriority {
    public String targetId;
    public Map<String, Double> factors = new LinkedHashMap<>();
    public double score;

    public TargetPriority() {
    }

    public TargetPriority(String targetId, Map<String, Double> factors, double score) {
        this.targetId = targetId;
        this.factors = new LinkedHashMap<>(factors);
        this.score = score;
    }
}

