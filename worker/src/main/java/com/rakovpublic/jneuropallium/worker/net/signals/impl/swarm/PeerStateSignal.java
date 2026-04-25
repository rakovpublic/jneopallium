/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm.AgentRole;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Role / battery / health snapshot broadcast by a peer.
 * ProcessingFrequency: loop=2, epoch=1.
 */
public class PeerStateSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 2);

    private String peerId;
    private AgentRole role;
    private double battery;
    private double health;
    private Map<String, Double> capabilities;

    public PeerStateSignal() {
        super();
        this.loop = 2;
        this.epoch = 1L;
        this.timeAlive = 300;
        this.role = AgentRole.IDLE;
        this.capabilities = new HashMap<>();
    }

    public PeerStateSignal(String peerId, AgentRole role, double battery, double health,
                           Map<String, Double> capabilities) {
        this();
        this.peerId = peerId;
        this.role = role == null ? AgentRole.IDLE : role;
        this.battery = Math.max(0.0, Math.min(1.0, battery));
        this.health = Math.max(0.0, Math.min(1.0, health));
        this.capabilities = capabilities == null ? new HashMap<>() : new HashMap<>(capabilities);
    }

    public String getPeerId() { return peerId; }
    public void setPeerId(String p) { this.peerId = p; }
    public AgentRole getRole() { return role; }
    public void setRole(AgentRole r) { this.role = r == null ? AgentRole.IDLE : r; }
    public double getBattery() { return battery; }
    public void setBattery(double b) { this.battery = Math.max(0.0, Math.min(1.0, b)); }
    public double getHealth() { return health; }
    public void setHealth(double h) { this.health = Math.max(0.0, Math.min(1.0, h)); }
    public Map<String, Double> getCapabilities() { return Collections.unmodifiableMap(capabilities); }
    public void setCapabilities(Map<String, Double> c) {
        this.capabilities = c == null ? new HashMap<>() : new HashMap<>(c);
    }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return PeerStateSignal.class; }
    @Override public String getDescription() { return "PeerStateSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        PeerStateSignal c = new PeerStateSignal(peerId, role, battery, health, capabilities);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
