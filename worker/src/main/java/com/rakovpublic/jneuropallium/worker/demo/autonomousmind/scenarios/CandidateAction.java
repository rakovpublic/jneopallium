package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.scenarios;

import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.ActionType;

import java.util.LinkedHashMap;
import java.util.Map;

public class CandidateAction {
    public ActionType action;
    public double planScore;
    public SafetyDecision safetyDecision;

    public CandidateAction(ActionType action, double planScore) {
        this.action = action;
        this.planScore = planScore;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("action", action.name());
        row.put("planScore", Math.round(planScore * 1000.0) / 1000.0);
        if (safetyDecision != null) {
            row.put("harmVerdict", safetyDecision.verdict.name());
            row.put("vetoReason", safetyDecision.reason);
            row.put("harmDimension", safetyDecision.dimension.name());
            row.put("safeAlternative", safetyDecision.safeAlternative == null ? null : safetyDecision.safeAlternative.name());
        }
        return row;
    }
}
