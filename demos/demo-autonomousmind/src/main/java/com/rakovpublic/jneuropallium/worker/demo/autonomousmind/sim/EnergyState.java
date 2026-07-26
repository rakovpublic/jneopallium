package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.sim;

import java.util.LinkedHashMap;
import java.util.Map;

public class EnergyState {
    public double level;
    public boolean charging;
    public boolean docked;

    public EnergyState() {
    }

    public EnergyState(double level, boolean charging) {
        this.level = level;
        this.charging = charging;
        this.docked = charging;
    }

    public void consume(double amount) {
        if (!charging) {
            level = Math.max(0.0, level - amount);
        }
    }

    public void charge(double amount) {
        charging = true;
        docked = true;
        level = Math.min(100.0, level + amount);
    }

    public String chargingState() {
        if (charging && docked) {
            return "DOCKED_CHARGING";
        }
        return charging ? "CHARGING" : "DISCHARGING";
    }

    public Map<String, Object> toMap() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("level", Math.round(level * 1000.0) / 1000.0);
        row.put("charging", charging);
        row.put("docked", docked);
        row.put("chargingState", chargingState());
        return row;
    }
}
