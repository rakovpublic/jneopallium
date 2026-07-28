package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.sim;

import java.util.LinkedHashMap;
import java.util.Map;

public class ChargingStation {
    public String stationId = "charger-1";
    public int x;
    public int y;
    public double chargePerTick = 18.0;

    public ChargingStation() {
    }

    public ChargingStation(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("stationId", stationId);
        row.put("x", x);
        row.put("y", y);
        row.put("chargePerTick", chargePerTick);
        return row;
    }
}
