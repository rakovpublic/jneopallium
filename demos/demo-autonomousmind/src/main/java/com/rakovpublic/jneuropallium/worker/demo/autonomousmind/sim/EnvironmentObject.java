package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.sim;

import java.util.LinkedHashMap;
import java.util.Map;

public class EnvironmentObject {
    public String objectId;
    public String objectType;
    public int x;
    public int y;
    public boolean inspectable = true;
    public boolean fragile;
    public boolean privateData;

    public EnvironmentObject() {
    }

    public EnvironmentObject(String objectId, String objectType, int x, int y) {
        this.objectId = objectId;
        this.objectType = objectType;
        this.x = x;
        this.y = y;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("objectId", objectId);
        row.put("objectType", objectType);
        row.put("x", x);
        row.put("y", y);
        row.put("inspectable", inspectable);
        row.put("fragile", fragile);
        row.put("privateData", privateData);
        return row;
    }
}
