package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.sim;

import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.CognitiveMode;

import java.util.LinkedHashMap;
import java.util.Map;

public class AgentOperationalState {
    public int x;
    public int y;
    public CognitiveMode mode = CognitiveMode.TASK_MODE;
    public String activeTaskId;
    public String activeSubgoal;
    public String pausedTaskId;
    public double taskProgress;
    public double mapCoverage;
    public boolean reportGenerated;
    public boolean taskStatePreserved;

    public AgentOperationalState() {
    }

    public AgentOperationalState(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void moveTo(int nextX, int nextY) {
        this.x = nextX;
        this.y = nextY;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("x", x);
        row.put("y", y);
        row.put("mode", mode.name());
        row.put("activeTaskId", activeTaskId);
        row.put("activeSubgoal", activeSubgoal);
        row.put("pausedTaskId", pausedTaskId);
        row.put("taskProgress", round(taskProgress));
        row.put("mapCoverage", round(mapCoverage));
        row.put("reportGenerated", reportGenerated);
        row.put("taskStatePreserved", taskStatePreserved);
        return row;
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
