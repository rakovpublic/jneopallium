/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm;

/**
 * Configuration for the swarm-robotics module. Mirrors spec §9.
 * Per spec §12: lethal autonomous weapons are out of scope and the
 * affect module must not be enabled in this module — both are
 * exposed as program-level read-only flags.
 */
public final class SwarmConfig {

    private boolean enabled = true;
    private String agentId = "bot-000";

    // transport
    private String primaryTransport = "dds";
    private String fallbackTransport = "mqtt";
    private boolean loraMesh = true;
    private int bandwidthKbps = 100;

    // neighbourhood
    private int maxPeers = 12;
    private long stalenessTicks = 300;

    // consensus
    private String consensusProtocol = "gossip-crdt";
    private int quorumWitnessCount = 3;

    // auction
    private long bidTimeoutTicks = 50;
    private String tieBreaker = "lowest-id";

    // flocking
    private double separation = 1.0;
    private double alignment = 0.7;
    private double cohesion = 0.5;
    private double radius = 5.0;

    // stigmergy
    private boolean stigmergyEnabled = true;
    private long defaultDecayTicks = 1000;

    // byzantine
    private int anomalyThresholdVotes = 3;
    private long isolationTicks = 600;
    private int maxReputationHistory = 1000;

    // collective harm
    private String regionalThresholdConfig = "regional-thresholds.yaml";
    private String aggregatorElection = "raft-lite";

    // curiosity
    private boolean curiosityEnabled = true;
    private boolean roleDifferentiation = true;

    // hard out-of-scope flags
    private final boolean lawsEnabled = false;       // lethal autonomous weapons — fixed off
    private final boolean affectEnabled = false;     // affect module — fixed off in swarm

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { this.enabled = v; }
    public String getAgentId() { return agentId; }
    public void setAgentId(String s) { this.agentId = s; }

    public String getPrimaryTransport() { return primaryTransport; }
    public void setPrimaryTransport(String s) { this.primaryTransport = s; }
    public String getFallbackTransport() { return fallbackTransport; }
    public void setFallbackTransport(String s) { this.fallbackTransport = s; }
    public boolean isLoraMesh() { return loraMesh; }
    public void setLoraMesh(boolean v) { this.loraMesh = v; }
    public int getBandwidthKbps() { return bandwidthKbps; }
    public void setBandwidthKbps(int v) { this.bandwidthKbps = Math.max(1, v); }

    public int getMaxPeers() { return maxPeers; }
    public void setMaxPeers(int v) { this.maxPeers = Math.max(1, v); }
    public long getStalenessTicks() { return stalenessTicks; }
    public void setStalenessTicks(long v) { this.stalenessTicks = Math.max(1L, v); }

    public String getConsensusProtocol() { return consensusProtocol; }
    public void setConsensusProtocol(String s) { this.consensusProtocol = s; }
    public int getQuorumWitnessCount() { return quorumWitnessCount; }
    public void setQuorumWitnessCount(int v) {
        // Spec §7: "minimum 3 for Byzantine f=1 tolerance on a small swarm".
        if (v < 3) throw new IllegalArgumentException("quorum-witness-count must be ≥ 3");
        this.quorumWitnessCount = v;
    }

    public long getBidTimeoutTicks() { return bidTimeoutTicks; }
    public void setBidTimeoutTicks(long v) { this.bidTimeoutTicks = Math.max(1L, v); }
    public String getTieBreaker() { return tieBreaker; }
    public void setTieBreaker(String s) { this.tieBreaker = s; }

    public double getSeparation() { return separation; }
    public void setSeparation(double v) { this.separation = v; }
    public double getAlignment() { return alignment; }
    public void setAlignment(double v) { this.alignment = v; }
    public double getCohesion() { return cohesion; }
    public void setCohesion(double v) { this.cohesion = v; }
    public double getRadius() { return radius; }
    public void setRadius(double v) { this.radius = Math.max(0.0, v); }

    public boolean isStigmergyEnabled() { return stigmergyEnabled; }
    public void setStigmergyEnabled(boolean v) { this.stigmergyEnabled = v; }
    public long getDefaultDecayTicks() { return defaultDecayTicks; }
    public void setDefaultDecayTicks(long v) { this.defaultDecayTicks = Math.max(1L, v); }

    public int getAnomalyThresholdVotes() { return anomalyThresholdVotes; }
    public void setAnomalyThresholdVotes(int v) {
        if (v < 3) throw new IllegalArgumentException("anomaly-threshold-votes must be ≥ 3 (Byzantine f=1)");
        this.anomalyThresholdVotes = v;
    }
    public long getIsolationTicks() { return isolationTicks; }
    public void setIsolationTicks(long v) { this.isolationTicks = Math.max(1L, v); }
    public int getMaxReputationHistory() { return maxReputationHistory; }
    public void setMaxReputationHistory(int v) { this.maxReputationHistory = Math.max(1, v); }

    public String getRegionalThresholdConfig() { return regionalThresholdConfig; }
    public void setRegionalThresholdConfig(String s) { this.regionalThresholdConfig = s; }
    public String getAggregatorElection() { return aggregatorElection; }
    public void setAggregatorElection(String s) { this.aggregatorElection = s; }

    public boolean isCuriosityEnabled() { return curiosityEnabled; }
    public void setCuriosityEnabled(boolean v) { this.curiosityEnabled = v; }
    public boolean isRoleDifferentiation() { return roleDifferentiation; }
    public void setRoleDifferentiation(boolean v) { this.roleDifferentiation = v; }

    /** Lethal autonomous weapons — fixed off. */
    public boolean isLawsEnabled() { return lawsEnabled; }
    /** Affect module is intentionally not used in swarm mode (spec §12). */
    public boolean isAffectEnabled() { return affectEnabled; }
}
