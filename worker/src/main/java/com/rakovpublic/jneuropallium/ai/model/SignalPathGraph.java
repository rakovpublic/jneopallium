package com.rakovpublic.jneuropallium.ai.model;

import java.util.*;

public class SignalPathGraph {
    private final Map<String, Map<String, EdgeInfo>> adjacency = new HashMap<>();

    private static class EdgeInfo {
        double weight;
        int addedTick;
        EdgeInfo(double weight, int tick) { this.weight = weight; this.addedTick = tick; }
    }

    public void addEdge(String from, String to, double weight) {
        adjacency.computeIfAbsent(from, k -> new HashMap<>())
                 .put(to, new EdgeInfo(weight, 0));
    }

    public void addEdge(String from, String to, double weight, int currentTick) {
        adjacency.computeIfAbsent(from, k -> new HashMap<>())
                 .put(to, new EdgeInfo(weight, currentTick));
    }

    public void expireEdges(int currentTick, int maxAge) {
        for (Map<String, EdgeInfo> targets : adjacency.values()) {
            targets.entrySet().removeIf(e -> (currentTick - e.getValue().addedTick) > maxAge);
        }
    }

    public Map<String, Double> getNeighbours(String nodeId) {
        Map<String, EdgeInfo> info = adjacency.getOrDefault(nodeId, Collections.emptyMap());
        Map<String, Double> result = new HashMap<>();
        for (Map.Entry<String, EdgeInfo> e : info.entrySet()) result.put(e.getKey(), e.getValue().weight);
        return result;
    }

    public Set<String> getNodes() { return adjacency.keySet(); }

    /** Returns all cycles as lists of node IDs using DFS. */
    public List<List<String>> findCycles() {
        List<List<String>> cycles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> inStack = new HashSet<>();
        Map<String, String> parent = new HashMap<>();
        for (String node : adjacency.keySet()) {
            if (!visited.contains(node)) {
                dfs(node, visited, inStack, parent, cycles);
            }
        }
        return cycles;
    }

    private void dfs(String node, Set<String> visited, Set<String> inStack, Map<String, String> parent, List<List<String>> cycles) {
        visited.add(node);
        inStack.add(node);
        Map<String, EdgeInfo> neighbours = adjacency.getOrDefault(node, Collections.emptyMap());
        for (String neighbour : neighbours.keySet()) {
            if (!visited.contains(neighbour)) {
                parent.put(neighbour, node);
                dfs(neighbour, visited, inStack, parent, cycles);
            } else if (inStack.contains(neighbour)) {
                List<String> cycle = new ArrayList<>();
                cycle.add(neighbour);
                String cur = node;
                while (!cur.equals(neighbour)) {
                    cycle.add(0, cur);
                    cur = parent.getOrDefault(cur, "");
                    if (cur.isEmpty()) break;
                }
                if (!cur.isEmpty()) cycles.add(cycle);
            }
        }
        inStack.remove(node);
    }

    public double getEdgeWeight(String from, String to) {
        EdgeInfo info = adjacency.getOrDefault(from, Collections.emptyMap()).get(to);
        return info != null ? info.weight : 0.0;
    }

    public void updateEdgeWeight(String from, String to, double weight) {
        Map<String, EdgeInfo> targets = adjacency.get(from);
        if (targets != null && targets.containsKey(to)) {
            targets.get(to).weight = weight;
        }
    }
}
