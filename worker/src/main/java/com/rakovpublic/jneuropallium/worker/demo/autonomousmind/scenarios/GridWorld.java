package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.scenarios;

import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.sim.BystanderState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GridWorld {
    public final List<String> originalRows = new ArrayList<>();
    public GridCell[][] cells;
    public AgentState agent;
    public BystanderState bystander;
    public FragileObjectState fragileObject;
    public final Set<String> foodCells = new LinkedHashSet<>();
    public int lavaEntries;
    public boolean bystanderUnharmed = true;
    public boolean bystanderPathAvailable = true;

    public static GridWorld fromRows(List<String> rows, double initialEnergy) {
        GridWorld world = new GridWorld();
        world.originalRows.addAll(rows);
        int height = rows.size();
        int width = rows.stream().mapToInt(String::length).max().orElse(0);
        world.cells = new GridCell[height][width];
        for (int y = 0; y < height; y++) {
            String row = rows.get(y);
            for (int x = 0; x < width; x++) {
                char symbol = x < row.length() ? row.charAt(x) : '#';
                CellType type = typeFor(symbol);
                world.cells[y][x] = new GridCell(x, y, type);
                if (symbol == 'A') {
                    world.agent = new AgentState(x, y, initialEnergy);
                    world.cells[y][x].type = CellType.EMPTY;
                } else if (symbol == 'F') {
                    world.foodCells.add(key(x, y));
                } else if (symbol == 'B') {
                    world.bystander = new BystanderState(x, y);
                    world.cells[y][x].type = CellType.EMPTY;
                } else if (symbol == 'O') {
                    world.fragileObject = new FragileObjectState(x, y);
                    world.cells[y][x].type = CellType.EMPTY;
                }
            }
        }
        if (world.agent == null) {
            world.agent = new AgentState(1, 1, initialEnergy);
        }
        return world;
    }

    public GridCell cell(int x, int y) {
        if (cells == null || y < 0 || y >= cells.length || x < 0 || x >= cells[0].length) {
            return new GridCell(x, y, CellType.WALL);
        }
        return cells[y][x];
    }

    public boolean hasFood(int x, int y) {
        return foodCells.contains(key(x, y));
    }

    public void removeFood(int x, int y) {
        foodCells.remove(key(x, y));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("grid", render());
        row.put("agent", agent.toMap());
        row.put("bystander", bystander == null ? null : bystander.toMap());
        row.put("fragileObject", fragileObject == null ? null : fragileObject.toMap());
        row.put("foodRemaining", foodCells.size());
        row.put("lavaEntries", lavaEntries);
        row.put("bystanderUnharmed", bystanderUnharmed);
        row.put("bystanderPathAvailable", bystanderPathAvailable);
        return row;
    }

    public List<String> render() {
        List<String> rows = new ArrayList<>();
        for (int y = 0; y < cells.length; y++) {
            StringBuilder row = new StringBuilder();
            for (int x = 0; x < cells[y].length; x++) {
                if (agent != null && agent.x == x && agent.y == y) {
                    row.append('A');
                } else if (bystander != null && bystander.x == x && bystander.y == y) {
                    row.append('B');
                } else if (fragileObject != null && fragileObject.x == x && fragileObject.y == y) {
                    row.append('O');
                } else if (hasFood(x, y)) {
                    row.append('F');
                } else {
                    row.append(symbolFor(cells[y][x].type));
                }
            }
            rows.add(row.toString());
        }
        return rows;
    }

    private static CellType typeFor(char symbol) {
        return switch (symbol) {
            case '#' -> CellType.WALL;
            case 'F' -> CellType.FOOD;
            case 'L' -> CellType.LAVA;
            case 'O' -> CellType.FRAGILE_OBJECT;
            case 'B' -> CellType.BYSTANDER;
            case '?' -> CellType.UNKNOWN;
            case 'M' -> CellType.MOVING_OBSTACLE;
            case 'G' -> CellType.GOAL;
            default -> CellType.EMPTY;
        };
    }

    private static char symbolFor(CellType type) {
        return switch (type) {
            case WALL -> '#';
            case FOOD -> 'F';
            case LAVA -> 'L';
            case UNKNOWN -> '?';
            case MOVING_OBSTACLE -> 'M';
            case GOAL -> 'G';
            default -> '.';
        };
    }

    public static String key(int x, int y) {
        return x + "," + y;
    }
}
