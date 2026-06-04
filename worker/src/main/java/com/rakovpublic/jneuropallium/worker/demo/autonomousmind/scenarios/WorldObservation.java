package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.scenarios;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WorldObservation {
    public int tick;
    public List<String> localPatch = List.of();
    public String attendedObject;
    public double salience;
    public double uncertainty;

    public Map<String, Object> toMap() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("tick", tick);
        row.put("localPatch", localPatch);
        row.put("attendedObject", attendedObject);
        row.put("salience", Math.round(salience * 1000.0) / 1000.0);
        row.put("uncertainty", Math.round(uncertainty * 1000.0) / 1000.0);
        return row;
    }
}
