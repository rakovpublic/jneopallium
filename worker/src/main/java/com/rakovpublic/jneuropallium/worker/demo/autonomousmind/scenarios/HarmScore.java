package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.scenarios;

import java.util.LinkedHashMap;
import java.util.Map;

public class HarmScore {
    public HarmDimension dimension = HarmDimension.none;
    public double score;
    public String reason = "no projected harm";

    public Map<String, Object> toMap() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("dimension", dimension.name());
        row.put("score", Math.round(score * 1000.0) / 1000.0);
        row.put("reason", reason);
        return row;
    }
}
