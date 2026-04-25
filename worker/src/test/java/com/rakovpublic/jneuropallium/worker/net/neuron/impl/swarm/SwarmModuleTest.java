/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.AgentAnomalySignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.ConsensusProposalSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.ConsensusVoteSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.FormationSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.PeerObservationSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.PeerStateSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.PheromoneSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.StigmergicTraceSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.SwarmAlertSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.TaskAnnouncementSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.TaskAssignmentSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.TaskBidSignal;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.swarm.AgentAnomalyIsolationProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.swarm.ConsensusProposalProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.swarm.ConsensusVoteProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.swarm.FormationKeepingProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.swarm.PeerObservationIntegrationProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.swarm.PeerStateEnergyProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.swarm.PeerStateIntegrationProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.swarm.PeerStateRoleProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.swarm.PheromoneDepositProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.swarm.StigmergicTraceDepositProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.swarm.SwarmAlertProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.swarm.TaskAnnouncementBidProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.swarm.TaskAnnouncementRegistryProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.swarm.TaskAssignmentRegistryProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.swarm.TaskBidRegistryProcessor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SwarmModuleTest {

    // ---------- enums ----------
    @Test
    void enums_cardinality() {
        assertEquals(6, AgentRole.values().length);
        assertEquals(7, TaskKind.values().length);
        assertEquals(5, PheromoneKind.values().length);
        assertEquals(3, VoteKind.values().length);
        assertEquals(5, AlertCategory.values().length);
        assertEquals(5, AnomalyKind.values().length);
        assertEquals(5, TraceKind.values().length);
        assertEquals(7, FormationTemplate.values().length);
    }

    // ---------- ProcessingFrequency ----------
    @Test
    void signals_frequencies() {
        assertEquals(2L, PeerObservationSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(1L, PeerStateSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(2, PeerStateSignal.PROCESSING_FREQUENCY.getLoop());
        assertEquals(1L, TaskAnnouncementSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(1L, TaskBidSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(1L, TaskAssignmentSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(2L, PheromoneSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(2L, FormationSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(1L, ConsensusProposalSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(1L, ConsensusVoteSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(1L, SwarmAlertSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(1L, AgentAnomalySignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(5L, StigmergicTraceSignal.PROCESSING_FREQUENCY.getEpoch());
    }

    // ---------- Layer 0 ----------
    @Test
    void peerObservation_dropsLowLinkQuality() {
        PeerObservationNeuron n = new PeerObservationNeuron();
        n.setMinLinkQuality(0.5);
        assertNotNull(n.observe("p1", new double[]{1, 2}, new double[]{0, 0}, 0.7));
        assertNull(n.observe("p2", null, null, 0.2));
    }

    @Test
    void meshRadio_dropsBelowThreshold() {
        MeshRadioNeuron r = new MeshRadioNeuron();
        r.setLossThreshold(0.3);
        assertTrue(r.send(new SwarmAlertSignal(AlertCategory.COLLISION, "r", 0.9), 0.5));
        assertFalse(r.send(new SwarmAlertSignal(AlertCategory.COLLISION, "r", 0.9), 0.1));
        assertEquals(1, r.getSent());
        assertEquals(1, r.getDropped());
    }

    // ---------- Layer 2 ----------
    @Test
    void peerState_evictsStale() {
        PeerStateIntegrationNeuron p = new PeerStateIntegrationNeuron();
        p.setStalenessTicks(100);
        p.onObservation(new PeerObservationSignal("p", new double[]{0}, new double[]{0}, 1.0), 0L);
        assertEquals(1, p.knownPeers().size());
        p.evict(500L);
        assertEquals(0, p.knownPeers().size());
    }

    @Test
    void roleAwareness_pickShortageRole() {
        RoleAwarenessNeuron r = new RoleAwarenessNeuron();
        r.onPeerState(new PeerStateSignal("a", AgentRole.SCOUT, 1.0, 1.0, null));
        r.onPeerState(new PeerStateSignal("b", AgentRole.SCOUT, 1.0, 1.0, null));
        r.onPeerState(new PeerStateSignal("c", AgentRole.SCOUT, 1.0, 1.0, null));
        AgentRole shortage = r.shortageRole();
        assertNotNull(shortage);
        assertNotEquals(AgentRole.SCOUT, shortage);
    }

    // ---------- Layer 3 ----------
    @Test
    void stigmergicMemory_evictsExpired() {
        StigmergicMemoryNeuron m = new StigmergicMemoryNeuron();
        m.deposit(new PheromoneSignal(PheromoneKind.TRAIL, new double[]{0, 0}, 1.0, 100L));
        assertEquals(1, m.size());
        m.evict(200L);
        assertEquals(0, m.size());
    }

    @Test
    void stigmergicMemory_findsNearby() {
        StigmergicMemoryNeuron m = new StigmergicMemoryNeuron();
        m.deposit(new StigmergicTraceSignal(new double[]{0, 0}, TraceKind.LANDMARK, 0.9, 0));
        m.deposit(new StigmergicTraceSignal(new double[]{50, 50}, TraceKind.HAZARD, 0.7, 0));
        assertEquals(1, m.tracesNear(new double[]{1, 1}, 5.0).size());
    }

    @Test
    void taskRegistry_assignClosesOpen() {
        TaskRegistryNeuron t = new TaskRegistryNeuron();
        t.register(new TaskAnnouncementSignal("T1", TaskKind.SEARCH, null, 1.0, 100));
        assertTrue(t.isOpen("T1"));
        t.assign(new TaskAssignmentSignal("T1", "a", "b"));
        assertFalse(t.isOpen("T1"));
        assertEquals("a", t.assigneeOf("T1"));
    }

    // ---------- Layer 4 ----------
    @Test
    void auctionBid_costScalesWithDistance() {
        AuctionBiddingNeuron a = new AuctionBiddingNeuron();
        a.setBidderId("self");
        a.setSelfPosition(new double[]{0, 0});
        a.setBatteryFraction(1.0);
        TaskAnnouncementSignal ann = new TaskAnnouncementSignal("T", TaskKind.SEARCH,
                new double[]{3, 4}, 1.0, 100);
        TaskBidSignal b = a.bid(ann);
        assertNotNull(b);
        assertEquals(5.0, b.getEstimatedCost(), 1e-6);
    }

    @Test
    void consensus_quorumOnYesCount() {
        ConsensusParticipantNeuron c = new ConsensusParticipantNeuron();
        c.setQuorum(3);
        c.tally(new ConsensusVoteSignal("p", VoteKind.YES, "v1"));
        c.tally(new ConsensusVoteSignal("p", VoteKind.YES, "v2"));
        assertFalse(c.isQuorum("p"));
        c.tally(new ConsensusVoteSignal("p", VoteKind.YES, "v3"));
        assertTrue(c.isQuorum("p"));
        // Duplicate voter doesn't double-count
        c.tally(new ConsensusVoteSignal("p", VoteKind.YES, "v3"));
        assertEquals(3, c.yesCount("p"));
    }

    @Test
    void formation_steerToOffset() {
        FormationKeepingNeuron f = new FormationKeepingNeuron();
        f.setSlot(new FormationSignal(FormationTemplate.LINE, 0, new double[]{10, 0}));
        double[] s = f.steer(new double[]{4, 0});
        assertEquals(6.0, s[0], 1e-9);
        assertEquals(0.0, s[1], 1e-9);
    }

    @Test
    void flocking_zeroOnEmpty() {
        FlockingNeuron f = new FlockingNeuron();
        double[] v = f.steer(new ArrayList<>());
        assertEquals(0, v.length);
    }

    @Test
    void flocking_separationOpposesNeighbour() {
        FlockingNeuron f = new FlockingNeuron();
        f.setWeights(1.0, 0.0, 0.0);
        f.setRadius(10.0);
        List<PeerObservationSignal> peers = new ArrayList<>();
        peers.add(new PeerObservationSignal("p", new double[]{2, 0}, new double[]{0, 0}, 1.0));
        double[] s = f.steer(peers);
        assertTrue(s[0] < 0, "separation should push away from a neighbour at +x");
    }

    // ---------- Layer 5 ----------
    @Test
    void swarmHarmGate_aggregatesAboveThreshold() {
        SwarmHarmGateNeuron g = new SwarmHarmGateNeuron();
        g.setRegionalThreshold("R", 1.0);
        g.recordEmission("R", "a1", 0.6);
        g.recordEmission("R", "a2", 0.6);
        SwarmAlertSignal alert = g.aggregate("R");
        assertNotNull(alert);
        assertEquals(AlertCategory.COLLECTIVE_HARM, alert.getCategory());
        assertTrue(g.tighteningMultiplier("R") > 1.0);
    }

    @Test
    void anomalyReport_singletonOncePerSuspect() {
        AnomalyReportNeuron r = new AnomalyReportNeuron();
        r.setSelfId("me");
        assertNotNull(r.report("susp", AnomalyKind.SILENT));
        assertNull(r.report("susp", AnomalyKind.SILENT));
    }

    @Test
    void isolation_kWitnessThreshold() {
        IsolationProtocolNeuron iso = new IsolationProtocolNeuron();
        iso.setWitnessThreshold(3);
        iso.setIsolationTicks(100);
        assertFalse(iso.onReport(new AgentAnomalySignal("susp", AnomalyKind.SILENT, "a",
                java.util.Arrays.asList("a")), 0));
        assertFalse(iso.onReport(new AgentAnomalySignal("susp", AnomalyKind.SILENT, "b",
                java.util.Arrays.asList("b")), 0));
        assertTrue(iso.onReport(new AgentAnomalySignal("susp", AnomalyKind.SILENT, "c",
                java.util.Arrays.asList("c")), 0));
        assertTrue(iso.isIsolated("susp", 50));
        // Auto-lift after isolationTicks
        iso.tick(200L);
        assertFalse(iso.isIsolated("susp", 200L));
    }

    // ---------- Layer 7 ----------
    @Test
    void density_highCountTriggersDispersal() {
        SwarmDensityNeuron d = new SwarmDensityNeuron();
        d.setMinNeighbours(2);
        d.setMaxNeighbours(4);
        for (int i = 0; i < 6; i++) d.observeNeighbour(null);
        assertTrue(d.densityBias() > 0);
    }

    @Test
    void bandwidth_rateLimitsLowPriority() {
        BandwidthBudgetNeuron b = new BandwidthBudgetNeuron();
        b.setBudgetKbps(1);  // 125 bytes/s budget
        long t = 0L;
        // Fill the budget with high-priority traffic.
        b.recordHighPriority(125, t);
        assertFalse(b.allowLowPriority(1, t));
        // Next second resets the budget.
        assertTrue(b.allowLowPriority(1, t + 1000L));
    }

    @Test
    void energyCoordinator_defersToHigherBatteryPeer() {
        EnergyCoordinatorNeuron e = new EnergyCoordinatorNeuron();
        e.setOwnBattery(0.20);
        e.onPeerState(new PeerStateSignal("p1", AgentRole.WORKER, 0.90, 1.0, null));
        e.onPeerState(new PeerStateSignal("p2", AgentRole.WORKER, 0.30, 1.0, null));
        assertEquals("p1", e.shouldDeferTo("T1"));
    }

    // ---------- Config ----------
    @Test
    void config_quorumMinIsThree() {
        SwarmConfig c = new SwarmConfig();
        assertThrows(IllegalArgumentException.class, () -> c.setQuorumWitnessCount(2));
        assertThrows(IllegalArgumentException.class, () -> c.setAnomalyThresholdVotes(2));
        c.setQuorumWitnessCount(5);
        assertEquals(5, c.getQuorumWitnessCount());
    }

    @Test
    void config_lawsHardcodedOff() {
        SwarmConfig c = new SwarmConfig();
        assertFalse(c.isLawsEnabled());
        assertFalse(c.isAffectEnabled());
    }

    // ---------- Processors: interface-typed + smoke ----------
    private static void assertInterfaceTyped(ISignalProcessor<?, ?> p) {
        assertTrue(p.getNeuronClass().isInterface(),
                p.getClass().getSimpleName() + " must target an interface");
        assertNotNull(p.getSignalClass());
        assertNotNull(p.getDescription());
        assertFalse(p.hasMerger());
    }

    @Test
    void processors_allInterfaceTyped() {
        assertInterfaceTyped(new PeerObservationIntegrationProcessor());
        assertInterfaceTyped(new PeerStateIntegrationProcessor());
        assertInterfaceTyped(new PeerStateRoleProcessor());
        assertInterfaceTyped(new PeerStateEnergyProcessor());
        assertInterfaceTyped(new TaskAnnouncementBidProcessor());
        assertInterfaceTyped(new TaskAnnouncementRegistryProcessor());
        assertInterfaceTyped(new TaskBidRegistryProcessor());
        assertInterfaceTyped(new TaskAssignmentRegistryProcessor());
        assertInterfaceTyped(new PheromoneDepositProcessor());
        assertInterfaceTyped(new StigmergicTraceDepositProcessor());
        assertInterfaceTyped(new FormationKeepingProcessor());
        assertInterfaceTyped(new ConsensusProposalProcessor());
        assertInterfaceTyped(new ConsensusVoteProcessor());
        assertInterfaceTyped(new SwarmAlertProcessor());
        assertInterfaceTyped(new AgentAnomalyIsolationProcessor());
    }

    @Test
    void taskAnnouncementBidProcessor_emitsBid() {
        AuctionBiddingNeuron a = new AuctionBiddingNeuron();
        a.setBidderId("self");
        a.setSelfPosition(new double[]{0, 0});
        a.setBatteryFraction(0.8);
        TaskAnnouncementBidProcessor p = new TaskAnnouncementBidProcessor();
        List<ISignal> out = p.process(new TaskAnnouncementSignal("T", TaskKind.SEARCH,
                new double[]{1, 0}, 1.0, 100), a);
        assertEquals(1, out.size());
        assertTrue(out.get(0) instanceof TaskBidSignal);
    }

    @Test
    void consensusProposalProcessor_emitsVote() {
        ConsensusParticipantNeuron c = new ConsensusParticipantNeuron();
        c.setVoterId("v1");
        ConsensusProposalProcessor p = new ConsensusProposalProcessor();
        List<ISignal> out = p.process(new ConsensusProposalSignal("p", "S", "leader"), c);
        assertEquals(1, out.size());
        assertEquals(VoteKind.YES, ((ConsensusVoteSignal) out.get(0)).getVote());
    }

    @Test
    void agentAnomalyIsolationProcessor_appliesIsolationAtThreshold() {
        IsolationProtocolNeuron iso = new IsolationProtocolNeuron();
        iso.setWitnessThreshold(2);
        AgentAnomalyIsolationProcessor p = new AgentAnomalyIsolationProcessor();
        AgentAnomalySignal r1 = new AgentAnomalySignal("susp", AnomalyKind.SILENT, "a",
                java.util.Arrays.asList("a"));
        AgentAnomalySignal r2 = new AgentAnomalySignal("susp", AnomalyKind.SILENT, "b",
                java.util.Arrays.asList("b"));
        r1.setEpoch(0L); r2.setEpoch(0L);
        p.process(r1, iso);
        p.process(r2, iso);
        assertTrue(iso.isIsolated("susp", 0L));
    }
}
