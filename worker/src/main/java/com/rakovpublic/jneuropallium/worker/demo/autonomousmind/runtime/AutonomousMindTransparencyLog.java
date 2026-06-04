package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

public class AutonomousMindTransparencyLog {
    public int tick;
    public String candidateAction;
    public String verdict;
    public String reason;
    public String executedAction;
    public String constraintFamily;
    public double projectedRisk;

    public Map<String, Object> toMap() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("tick", tick);
        row.put("candidateAction", candidateAction);
        row.put("verdict", verdict);
        row.put("reason", reason);
        row.put("executedAction", executedAction);
        row.put("constraintFamily", constraintFamily);
        row.put("projectedRisk", projectedRisk);
        row.put("preExecution", true);
        row.put("signal", "TransparencyLogSignal");
        return row;
    }
}
