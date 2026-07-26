package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.sim;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EnvironmentMap {
    public final List<String> originalRows = new ArrayList<>();
    public EnvironmentCell[][] cells;
    public final List<EnvironmentObject> objects = new ArrayList<>();
    public int agentX = 1;
    public int agentY = 1;
    public ChargingStation chargingStation = new ChargingStation(1, 1);
    public BystanderState bystander = new BystanderState(0, 0);

    public static EnvironmentMap fromRows(List<String> rows) {
        EnvironmentMap map = new EnvironmentMap();
        map.originalRows.addAll(rows);
        int height = rows.size();
        int width = rows.stream().mapToInt(String::length).max().orElse(0);
        map.cells = new EnvironmentCell[height][width];
        for (int y = 0; y < height; y++) {
            String row = rows.get(y);
            for (int x = 0; x < width; x++) {
                char symbol = x < row.length() ? row.charAt(x) : '#';
                EnvironmentCell cell = new EnvironmentCell(x, y, zoneFor(x, y));
                applySymbol(map, cell, symbol);
                map.cells[y][x] = cell;
            }
        }
        return map;
    }

    private static String zoneFor(int x, int y) {
        if (x >= 4 && y <= 4) {
            return "A";
        }
        if (x >= 6) {
            return "B";
        }
        return "base";
    }

    private static void applySymbol(EnvironmentMap map, EnvironmentCell cell, char symbol) {
        switch (symbol) {
            case '#' -> cell.obstacle = true;
            case '?' -> cell.unknown = true;
            case 'A' -> {
                map.agentX = cell.x;
                map.agentY = cell.y;
            }
            case 'C' -> {
                cell.chargingStation = true;
                map.chargingStation = new ChargingStation(cell.x, cell.y);
            }
            case 'H' -> map.bystander = new BystanderState(cell.x, cell.y);
            case 'O' -> {
                cell.objectToInspect = true;
                map.objects.add(new EnvironmentObject("inspection-object-" + map.objects.size(), "inspection-target", cell.x, cell.y));
            }
            case 'R' -> cell.radiationAnomaly = true;
            case 'T' -> cell.heatSource = true;
            case 'Q' -> cell.radioSource = true;
            case 'S' -> cell.soundSource = true;
            case 'P' -> cell.privacySensitive = true;
            case 'X' -> cell.forbiddenZone = true;
            default -> {
            }
        }
    }

    public EnvironmentCell cell(int x, int y) {
        if (cells == null || y < 0 || y >= cells.length || x < 0 || x >= cells[0].length) {
            EnvironmentCell wall = new EnvironmentCell(x, y, "out-of-bounds");
            wall.obstacle = true;
            return wall;
        }
        return cells[y][x];
    }

    public boolean canTraverse(int x, int y) {
        return cell(x, y).traversable() && !(bystander != null && bystander.x == x && bystander.y == y);
    }

    public Map<String, Object> summary() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("rows", originalRows);
        row.put("objects", objects.stream().map(EnvironmentObject::toMap).toList());
        row.put("chargingStation", chargingStation == null ? null : chargingStation.toMap());
        row.put("bystander", bystander == null ? null : bystander.toMap());
        return row;
    }
}
