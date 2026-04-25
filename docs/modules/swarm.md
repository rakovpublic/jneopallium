# Swarm Robotics & Distributed Multi-Agent Coordination

> Status: implementation of [`use-case-swarm-robotics.md`](../../use-case-swarm-robotics.md) for [jneopallium](https://github.com/rakovpublic/jneopallium).
> License: BSD 3-Clause.

---

## Abstract

Each agent runs a full jneopallium instance; the swarm is a network of
networks. Coordination is local-first: peer observations, role
awareness, stigmergic markers, auction bidding, light-weight consensus,
Reynolds flocking, formation keeping, Byzantine-tolerant isolation, and
collective-harm aggregation all reduce to typed signals exchanged over
realistic communication links. Communication is never assumed perfect
— every peer-bound signal carries a `linkQuality` indicator that
processors weight against.

## Design Principles

1. **Typed timescales.** Peer observations on loop 1 / epoch 2;
   peer state on 2/1; flocking on 1/1; pheromones on 2/2; stigmergic
   traces on 2/5; consensus and anomaly on 2/1; swarm alerts at 1/1
   so they propagate fast.
2. **Processors parameterised by interfaces.** Every
   `ISignalProcessor` under `worker/signalprocessor/impl/swarm`
   targets an `I<Neuron>` interface; concrete neurons can be swapped
   per-deployment without touching processors.
3. **Quarantine is never permanent locally.**
   `IsolationProtocolNeuron` automatically reaps expired isolations;
   repeats double the duration but only a global consensus decision
   removes a peer permanently.
4. **k-witness Byzantine tolerance.** `IsolationProtocolNeuron`
   refuses to isolate below the configured witness threshold.
   `SwarmConfig` enforces minimum 3 — `setQuorumWitnessCount(2)` and
   `setAnomalyThresholdVotes(2)` throw.
5. **No lethal autonomy.** `SwarmConfig.isLawsEnabled()` returns
   `false` and is fixed — no setter exposed.
6. **Communication realism.** `linkQuality ∈ [0,1]` on every peer
   signal; `MeshRadioNeuron` drops below threshold;
   `PeerObservationNeuron` filters at ingress.

## Signals

Package: `com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm`.

| Signal | Loop/Epoch | Notes |
|---|---|---|
| `PeerObservationSignal` | 1/2 | peerId, position / velocity (local), linkQuality |
| `PeerStateSignal` | 2/1 | peerId, AgentRole, battery, health, capabilities |
| `TaskAnnouncementSignal` | 2/1 | taskId, TaskKind, location, reward, deadline |
| `TaskBidSignal` | 2/1 | taskId, bidderId, cost, confidence |
| `TaskAssignmentSignal` | 2/1 | taskId, assigneeId, witnessId |
| `PheromoneSignal` | 2/2 | PheromoneKind, location, strength, decay |
| `FormationSignal` | 1/2 | FormationTemplate, slot, relative offset |
| `ConsensusProposalSignal` | 2/1 | proposalId, state, proposerId |
| `ConsensusVoteSignal` | 2/1 | proposalId, VoteKind, voterId |
| `SwarmAlertSignal` | 1/1 | AlertCategory, regionId, severity |
| `AgentAnomalySignal` | 2/1 | suspectId, AnomalyKind, detectorId, witnesses |
| `StigmergicTraceSignal` | 2/5 | location, TraceKind, saliency, deposited tick |

## Neurons

Each concrete neuron implements a matching `I<Neuron>` interface so
processors never depend on the concrete type.

### Layer 0 — peer sensing
- `PeerObservationNeuron` / `IPeerObservationNeuron` — drops
  observations below `minLinkQuality`.
- `MeshRadioNeuron` / `IMeshRadioNeuron` — link-quality-aware
  outbound transport adapter.

### Layer 2 — peer state
- `PeerStateIntegrationNeuron` / `IPeerStateIntegrationNeuron` —
  decay-based local view of neighbours.
- `RoleAwarenessNeuron` / `IRoleAwarenessNeuron` — distribution map
  + shortage-role bias.

### Layer 3 — collective memory
- `StigmergicMemoryNeuron` / `IStigmergicMemoryNeuron` — pheromone +
  trace cache with location-radius queries and decay-based eviction.
- `TaskRegistryNeuron` / `ITaskRegistryNeuron` — open / assigned tasks.

### Layer 4 — coordination planning
- `AuctionBiddingNeuron` / `IAuctionBiddingNeuron` — distance / battery
  cost model, declines bids when out of battery.
- `ConsensusParticipantNeuron` / `IConsensusParticipantNeuron` —
  unique-voter YES / NO tally; quorum = configurable k (≥ 3).
- `FormationKeepingNeuron` / `IFormationKeepingNeuron` — slot-offset
  steering.
- `FlockingNeuron` / `IFlockingNeuron` — Reynolds rules weighted by
  link quality.

### Layer 5 — collective safety
- `SwarmHarmGateNeuron` / `ISwarmHarmGateNeuron` — regional emission
  aggregator; emits `SwarmAlertSignal(COLLECTIVE_HARM)`; tightening
  multiplier ∈ [1, 5].
- `AnomalyReportNeuron` / `IAnomalyReportNeuron` — single-shot reports
  per suspect (no double-counting).
- `IsolationProtocolNeuron` / `IIsolationProtocolNeuron` — k-witness
  Byzantine isolation, automatic lift, exponential escalation up to
  cap.

### Layer 7 — homeostasis
- `SwarmDensityNeuron` / `ISwarmDensityNeuron` — disperse-or-aggregate
  bias.
- `BandwidthBudgetNeuron` / `IBandwidthBudgetNeuron` — token-bucket
  rate-limit on low-priority outbound traffic.
- `EnergyCoordinatorNeuron` / `IEnergyCoordinatorNeuron` — defers a
  task to a higher-battery peer when own battery falls below
  threshold.

## Processors

Package: `com.rakovpublic.jneuropallium.worker.signalprocessor.impl.swarm`.

| Signal | Processor(s) |
|---|---|
| `PeerObservationSignal` | `PeerObservationIntegrationProcessor` |
| `PeerStateSignal` | `PeerStateIntegrationProcessor`, `PeerStateRoleProcessor`, `PeerStateEnergyProcessor` |
| `TaskAnnouncementSignal` | `TaskAnnouncementBidProcessor`, `TaskAnnouncementRegistryProcessor` |
| `TaskBidSignal` | `TaskBidRegistryProcessor` |
| `TaskAssignmentSignal` | `TaskAssignmentRegistryProcessor` |
| `PheromoneSignal` | `PheromoneDepositProcessor` |
| `StigmergicTraceSignal` | `StigmergicTraceDepositProcessor` |
| `FormationSignal` | `FormationKeepingProcessor` |
| `ConsensusProposalSignal` | `ConsensusProposalProcessor` |
| `ConsensusVoteSignal` | `ConsensusVoteProcessor` |
| `SwarmAlertSignal` | `SwarmAlertProcessor` |
| `AgentAnomalySignal` | `AgentAnomalyIsolationProcessor` |

Every processor's `getNeuronClass()` returns an interface — the
`processors_allInterfaceTyped` test in `SwarmModuleTest` asserts this
invariant across all 15 processors.

## Configuration

`SwarmConfig` mirrors spec §9 with strongly-typed setters.

```yaml
swarm:
  enabled: true
  agent-id: "bot-042"
  transport:
    primary: dds
    fallback: mqtt
    lora-mesh: true
    bandwidth-kbps: 100
  neighbourhood:
    max-peers: 12
    staleness-ticks: 300
  consensus:
    protocol: gossip-crdt
    quorum-witness-count: 3       # setter throws on < 3
  auction:
    bid-timeout-ticks: 50
    tie-breaker: lowest-id
  flocking:
    weights:
      separation: 1.0
      alignment: 0.7
      cohesion: 0.5
    radius: 5.0
  stigmergy:
    enabled: true
    default-decay-ticks: 1000
  byzantine:
    anomaly-threshold-votes: 3    # setter throws on < 3
    isolation-ticks: 600
    max-reputation-history: 1000
  collective-harm:
    regional-threshold-config: "regional-thresholds.yaml"
    aggregator-election: raft-lite
  curiosity:
    enabled: true
    role-differentiation: true
```

## Tests

`SwarmModuleTest` (26 tests):

- enum cardinalities + `ProcessingFrequency` for every signal
- peer-observation drop on low link quality
- mesh radio drop on low link quality
- peer-state staleness eviction
- role-awareness shortage detection
- stigmergic decay-based eviction + radius-bounded retrieval
- task-registry assign closes open
- auction bid distance scaling
- consensus quorum on YES (unique voter dedupe)
- formation slot-offset steering
- flocking zero on empty + separation pushes away
- collective-harm regional aggregation + tightening multiplier
- single-shot anomaly reporting
- k-witness isolation at threshold + auto-lift after duration
- density bias for high count
- bandwidth budget rate-limits low priority
- energy coordinator defers to higher-battery peer
- config quorum minimum 3 (throws on lower)
- LAWS hard-coded off
- 15-processor `processors_allInterfaceTyped` invariant
- per-processor smoke tests (announcement → bid, proposal → vote,
  k-witness isolation through processor)

Full worker suite: 471/471 pass (445 prior + 26 new), no regressions.

## Validation (per spec §10)

1. *Communication robustness* — every peer-bound signal already
   carries `linkQuality`; processors weight by it. Drop-rate
   injection plugs in at the simulation harness.
2. *Partition tolerance* — each neuron is local-state only;
   reconciliation paths for `TaskRegistryNeuron` and
   `IsolationProtocolNeuron` are via consensus signals.
3. *Byzantine drill* — `IsolationProtocolNeuron` requires k
   independent witnesses; spec-mandated minimum 3 enforced by
   `SwarmConfig`.
4. *Scalability* — interface-typed neurons + per-processor stateless
   wiring keep per-tick overhead bounded; characterise per-deployment.
5. *Collective harm test* — `SwarmHarmGateNeuron.aggregate()` test
   confirms that 0.6 + 0.6 over a 1.0 threshold raises
   `COLLECTIVE_HARM` and bumps the tightening multiplier.
6. *Agent-loss resilience* — `PeerStateIntegrationNeuron.evict()`
   removes stale neighbours; downstream consumers see graceful
   degradation rather than blocked operation.

## Out of scope

Per spec §12:
- Lethal autonomous weapons. `SwarmConfig.isLawsEnabled()` is fixed
  off; no setter is exposed.
- Centralised training / execution.
- Adversarial-insider Byzantine tolerance — pair with the
  cybersecurity module.
- Affect-module emotional mimicry. `SwarmConfig.isAffectEnabled()`
  is fixed off.

## References

- Rakovskyi, D. (2024). *Framework for Natural Neuron Network
  Modeling: The Jneopallium Approach.* IJSR 13(7).
- Reynolds, C.W. (1987). Flocks, herds, and schools. *ACM SIGGRAPH.*
- Dorigo, M., Birattari, M., Stutzle, T. (2006). Ant colony
  optimization. *IEEE Computational Intelligence Magazine.*
- Camazine, S. et al. (2001). *Self-Organization in Biological
  Systems.* Princeton University Press.
- Brambilla, M., Ferrante, E., Birattari, M., Dorigo, M. (2013).
  Swarm robotics: a review from the swarm engineering perspective.
  *Swarm Intelligence* 7, 1–41.
- Castro, M., Liskov, B. (1999). Practical Byzantine Fault Tolerance.
  *OSDI.*
- Ongaro, D., Ousterhout, J. (2014). In search of an understandable
  consensus algorithm. *USENIX ATC.*
