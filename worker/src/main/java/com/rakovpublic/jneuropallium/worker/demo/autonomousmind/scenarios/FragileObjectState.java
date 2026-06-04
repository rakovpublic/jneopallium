package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.scenarios;

import java.util.LinkedHashMap;
import java.util.Map;

public class FragileObjectState {
    public int x;
    public int y;
    public boolean intact = true;

    public FragileObjectState() {
    }

    public FragileObjectState(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("x", x);
        row.put("y", y);
        row.put("intact", intact);
        return row;
    }
}
