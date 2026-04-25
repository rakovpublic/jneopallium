/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * Stateless signal processors for the swarm-robotics module. Every
 * processor's {@code getNeuronClass()} returns an {@code I<Neuron>}
 * interface from
 * {@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm};
 * concrete neurons are dependency-injected at network construction.
 *
 * <p>Signal → processor coverage:
 * <ul>
 *   <li>PeerObservationSignal → PeerObservationIntegrationProcessor</li>
 *   <li>PeerStateSignal → PeerStateIntegrationProcessor,
 *       PeerStateRoleProcessor, PeerStateEnergyProcessor</li>
 *   <li>TaskAnnouncementSignal → TaskAnnouncementBidProcessor,
 *       TaskAnnouncementRegistryProcessor</li>
 *   <li>TaskBidSignal → TaskBidRegistryProcessor</li>
 *   <li>TaskAssignmentSignal → TaskAssignmentRegistryProcessor</li>
 *   <li>PheromoneSignal → PheromoneDepositProcessor</li>
 *   <li>FormationSignal → FormationKeepingProcessor</li>
 *   <li>ConsensusProposalSignal → ConsensusProposalProcessor</li>
 *   <li>ConsensusVoteSignal → ConsensusVoteProcessor</li>
 *   <li>SwarmAlertSignal → SwarmAlertProcessor</li>
 *   <li>AgentAnomalySignal → AgentAnomalyIsolationProcessor</li>
 *   <li>StigmergicTraceSignal → StigmergicTraceDepositProcessor</li>
 * </ul>
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.swarm;
