package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.sim;

import java.util.LinkedHashMap;
import java.util.Map;

public class WorldSnapshot {
    public int tick;
    public AgentOperationalState agent;
    public EnergyState energy;
    public SensorObservationFrame observation;
    public String executedAction;

    public Map<String, Object> toMap() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("tick", tick);
        row.put("agent", agent == null ? null : agent.toMap());
        row.put("energy", energy == null ? null : energy.toMap());
        row.put("observation", observation == null ? null : observation.toMap());
        row.put("executedAction", executedAction);
        row.put("simOnly", true);
        return row;
    }
}
