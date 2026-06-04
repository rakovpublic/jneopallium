package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.scenarios;

import java.util.LinkedHashMap;
import java.util.Map;

public class AgentState {
    public int x;
    public int y;
    public double energy = 100.0;
    public double fatigue;
    public double stress;
    public double damage;
    public double reward;
    public String activeGoal = "collect reward safely";
    public double confidence = 0.9;
    public double uncertainty = 0.1;

    public AgentState() {
    }

    public AgentState(int x, int y, double energy) {
        this.x = x;
        this.y = y;
        this.energy = energy;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("x", x);
        row.put("y", y);
        row.put("energy", round(energy));
        row.put("fatigue", round(fatigue));
        row.put("stress", round(stress));
        row.put("damage", round(damage));
        row.put("reward", round(reward));
        row.put("activeGoal", activeGoal);
        row.put("confidence", round(confidence));
        row.put("uncertainty", round(uncertainty));
        return row;
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
