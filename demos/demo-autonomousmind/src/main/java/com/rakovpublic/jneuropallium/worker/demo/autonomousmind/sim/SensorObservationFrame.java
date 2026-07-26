package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.sim;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SensorObservationFrame {
    public int tick;
    public List<String> selectedSources = List.of();
    public Map<String, Double> confidenceBySource = new LinkedHashMap<>();
    public List<String> conflicts = List.of();
    public String activeHypothesis;
    public String payloadSummary;

    public Map<String, Object> toMap() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("tick", tick);
        row.put("selectedSources", selectedSources);
        row.put("confidenceBySource", confidenceBySource);
        row.put("conflicts", conflicts);
        row.put("activeHypothesis", activeHypothesis);
        row.put("payloadSummary", payloadSummary);
        return row;
    }
}
