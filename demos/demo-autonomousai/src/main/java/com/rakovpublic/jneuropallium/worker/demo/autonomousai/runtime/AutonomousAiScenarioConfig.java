package com.rakovpublic.jneuropallium.worker.demo.autonomousai.runtime;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AutonomousAiScenarioConfig {
    public String scenarioId;
    public String description;
    public long seed = 42L;
    public int maxTicks = 20;
    public int slowFastRatio = 10;
    public List<String> grid = new ArrayList<>();
    public Safety safety = new Safety();
    public Llm llm = new Llm();
    public Map<String, String> options = new LinkedHashMap<>();

    public static class Safety {
        public boolean hardConstraints = true;
        public boolean harmGateEnabled = true;
        public Map<String, Double> hardVetoThresholds = new LinkedHashMap<>();

        public Safety() {
            hardVetoThresholds.put("physicalIntegrity", 0.70);
            hardVetoThresholds.put("autonomy", 0.70);
            hardVetoThresholds.put("resource", 0.70);
            hardVetoThresholds.put("information", 0.70);
            hardVetoThresholds.put("emotional", 0.70);
        }
    }

    public static class Llm {
        public String mode = "disabled";
        public int timeoutMillis = 25;
    }
}
