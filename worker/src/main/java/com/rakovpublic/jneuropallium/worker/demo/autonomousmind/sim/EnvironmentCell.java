package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.sim;

import java.util.LinkedHashMap;
import java.util.Map;

public class EnvironmentCell {
    public int x;
    public int y;
    public String zone = "main";
    public boolean obstacle;
    public boolean unknown;
    public boolean radiationAnomaly;
    public boolean heatSource;
    public boolean radioSource;
    public boolean soundSource;
    public boolean objectToInspect;
    public boolean privacySensitive;
    public boolean forbiddenZone;
    public boolean chargingStation;

    public EnvironmentCell() {
    }

    public EnvironmentCell(int x, int y, String zone) {
        this.x = x;
        this.y = y;
        this.zone = zone;
    }

    public boolean traversable() {
        return !obstacle && !forbiddenZone && !radiationAnomaly;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("x", x);
        row.put("y", y);
        row.put("zone", zone);
        row.put("obstacle", obstacle);
        row.put("unknown", unknown);
        row.put("radiationAnomaly", radiationAnomaly);
        row.put("heatSource", heatSource);
        row.put("radioSource", radioSource);
        row.put("soundSource", soundSource);
        row.put("objectToInspect", objectToInspect);
        row.put("privacySensitive", privacySensitive);
        row.put("forbiddenZone", forbiddenZone);
        row.put("chargingStation", chargingStation);
        return row;
    }
}
