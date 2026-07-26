package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.scenarios;

import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.ActionType;

import java.util.LinkedHashMap;
import java.util.Map;

public class SafetyDecision {
    public HarmVerdict verdict = HarmVerdict.APPROVED;
    public HarmDimension dimension = HarmDimension.none;
    public String reason = "approved by pre-execution consequence model";
    public double harmScore;
    public ActionType safeAlternative;

    public Map<String, Object> toMap() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("verdict", verdict.name());
        row.put("dimension", dimension.name());
        row.put("reason", reason);
        row.put("harmScore", Math.round(harmScore * 1000.0) / 1000.0);
        row.put("safeAlternative", safeAlternative == null ? null : safeAlternative.name());
        return row;
    }
}
