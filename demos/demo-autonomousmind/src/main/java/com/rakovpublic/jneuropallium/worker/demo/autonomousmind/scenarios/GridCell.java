package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.scenarios;

import java.util.LinkedHashMap;
import java.util.Map;

public class GridCell {
    public final int x;
    public final int y;
    public CellType type;

    public GridCell(int x, int y, CellType type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public boolean traversable() {
        return type != CellType.WALL && type != CellType.LAVA && type != CellType.MOVING_OBSTACLE;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("x", x);
        row.put("y", y);
        row.put("type", type.name());
        row.put("traversable", traversable());
        return row;
    }
}
