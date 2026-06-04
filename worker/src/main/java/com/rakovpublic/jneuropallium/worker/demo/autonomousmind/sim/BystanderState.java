package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.sim;

import java.util.LinkedHashMap;
import java.util.Map;

public class BystanderState {
    public String humanId = "passive-human-1";
    public int x;
    public int y;
    public boolean unharmed = true;

    public BystanderState() {
    }

    public BystanderState(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("humanId", humanId);
        row.put("x", x);
        row.put("y", y);
        row.put("unharmed", unharmed);
        return row;
    }
}
