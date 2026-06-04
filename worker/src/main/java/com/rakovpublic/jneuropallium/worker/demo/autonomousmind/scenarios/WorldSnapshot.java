package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.scenarios;

import java.util.LinkedHashMap;
import java.util.Map;

public class WorldSnapshot {
    public int tick;
    public GridWorld world;
    public WorldObservation observation;
    public String executedAction;

    public Map<String, Object> toMap() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("tick", tick);
        row.put("world", world == null ? null : world.toMap());
        row.put("observation", observation == null ? null : observation.toMap());
        row.put("executedAction", executedAction);
        row.put("simOnly", true);
        return row;
    }
}
