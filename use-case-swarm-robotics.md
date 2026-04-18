# Use Case: Swarm Robotics & Distributed Multi-Agent Coordination

> **Framework:** [jneopallium](https://github.com/rakovpublic/jneopallium) + autonomous-AI architecture + embodiment + curiosity modules.
> **Domain:** Multi-robot search-and-rescue, inspection drones, warehouse AGV fleets, agricultural robot swarms, satellite constellations, distributed environmental monitoring.
> **Why jneopallium fits:** The framework's `INeuronNetInput` interface — feeding one neuron network's output into another's input — is already the right primitive for agent-to-agent communication. Its existing `NeuromodulatorSignal` broadcast pattern maps directly to pheromone-like stigmergic coordination. The cluster-gRPC mode provides the transport. Treating each robot as one neuron network and the swarm as a network-of-networks is a natural fit to what jneopallium already is.

---

## 1. Problem framing

Swarm coordination needs:

- **Local autonomy** — each agent must act on incomplete information with stale or no communication.
- **Emergent group behaviour** — coordination without a central planner.
- **Role differentiation** — specialists emerge where needed (scouts, workers, guards).
- **Resilience** — graceful degradation as agents drop out.
- **Communication discipline** — bandwidth and latency are both limited.
- **Collective safety** — no agent should cause harm, and the swarm should not collectively produce harm that no single agent intended.

Conventional multi-agent RL often assumes centralised training with decentralised execution and produces policies that silently depend on homogeneity. jneopallium's existing modular-network-boundary pattern (with `ModularGateNeuron` pairs) gives an honest, inspectable boundary between "what this agent knows and decides" and "what the swarm knows and decides."

---

## 2. Mapping to core framework

| Swarm concept | jneopallium primitive |
|---|---|
| One agent's cognition | One full jneopallium network instance (autonomous-AI architecture) |
| Swarm | Network of networks connected via `INeuronNetInput` |
| Peer message | `PeerSignal` type consumed by an agent's input layer |
| Pheromone / stigmergy | `NeuromodulatorSignal` broadcast to swarm region |
| Role specialisation | Weight differentiation in `HebbianLearningNeuron` under differential intrinsic reward |
| Task allocation | Distributed consensus via voting neurons |
| Formation maintenance | Reafference via `EfferenceCopySignal` correlated with neighbour position |
| Collective harm check | Additional `SwarmHarmNeuron` layer at regional level |
| Agent loss | Drop-out handled as `StructuralPlasticitySignal(deleteId)` at the swarm level |

---

## 3. Domain-specific signals

Package: `com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm`

| Signal | Loop / Epoch | Payload |
|---|---|---|
| `PeerObservationSignal` | 1 / 2 | `String peerId`, `double[] positionLocal`, `double[] velocityLocal`, `double rssi` or `linkQuality` |
| `PeerStateSignal` | 2 / 1 | `String peerId`, `AgentRole role`, `double battery`, `double health`, `Map<String,Double> capabilities` |
| `TaskAnnouncementSignal` | 2 / 1 | `String taskId`, `TaskKind kind`, `double[] locationGlobal`, `double reward`, `long deadlineTick` |
| `TaskBidSignal` | 2 / 1 | `String taskId`, `String bidderId`, `double estimatedCost`, `double confidence` |
| `TaskAssignmentSignal` | 2 / 1 | `String taskId`, `String assigneeId`, `String witnessId` |
| `PheromoneSignal` | 2 / 2 | `PheromoneKind kind`, `double[] locationGlobal`, `double strength`, `long decayTick` |
| `FormationSignal` | 1 / 2 | `FormationTemplate template`, `int slotIndex`, `double[] relativeOffset` |
| `ConsensusProposalSignal` | 2 / 1 | `String proposalId`, `Object proposedState`, `String proposerId` |
| `ConsensusVoteSignal` | 2 / 1 | `String proposalId`, `VoteKind vote` (YES, NO, ABSTAIN), `String voterId` |
| `SwarmAlertSignal` | 1 / 1 | `AlertCategory cat`, `String regionId`, `double severity` |
| `AgentAnomalySignal` | 2 / 1 | `String suspectId`, `AnomalyKind kind`, `String detectorId`, `List<String> witnesses` |
| `StigmergicTraceSignal` | 2 / 5 | `double[] locationGlobal`, `TraceKind kind`, `double saliency`, `long tickDeposited` |

### Communication realism

All peer signals carry `linkQuality` or `rssi` so that `PeerObservationProcessor` can weight neighbour contributions by connection reliability. Do not assume perfect communication. Drop-rate injection must be part of the test harness, not an afterthought.

---

## 4. Domain-specific neurons

Package: `com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm`

### Layer 0 — peer sensing

| Class | Loop / Epoch | Role |
|---|---|---|
| `PeerObservationNeuron` | 1 / 2 | Converts radio/LIDAR/vision detection of neighbours into `PeerObservationSignal` |
| `MeshRadioNeuron` | 1 / 1 | Transport adapter — MQTT, ZeroMQ, DDS, LoRa mesh; respects link-quality bias |

### Layer 2 — peer state

| Class | Loop / Epoch | Role |
|---|---|---|
| `PeerStateIntegrationNeuron` | 2 / 1 | Maintains local view of neighbour roles, health, positions; decays stale entries |
| `RoleAwarenessNeuron` | 2 / 2 | Knows own role; observes role distribution in local neighbourhood |

### Layer 3 — collective memory

| Class | Loop / Epoch | Role |
|---|---|---|
| `StigmergicMemoryNeuron` | 2 / 5 | Local cache of `StigmergicTraceSignal` entries — pheromone-like landmarks |
| `TaskRegistryNeuron` | 2 / 1 | Authoritative-ish list of active tasks; reconciled via consensus |

### Layer 4 — coordination planning

| Class | Loop / Epoch | Role |
|---|---|---|
| `AuctionBiddingNeuron` | 2 / 1 | Computes bids for `TaskAnnouncementSignal`; specialises `PlanningNeuron` with cost model |
| `ConsensusParticipantNeuron` | 2 / 1 | Implements light-weight consensus (Raft-lite or gossip-based CRDT reconciliation) |
| `FormationKeepingNeuron` | 1 / 2 | Maintains relative-position offset to leader or template slot |
| `FlockingNeuron` | 1 / 1 | Reynolds-rules primitives — separation, alignment, cohesion — emitted as fast-loop steering |

### Layer 5 — collective safety

| Class | Loop / Epoch | Role |
|---|---|---|
| `SwarmHarmGateNeuron` | 1 / 1 | Coordinates with neighbours' harm gates; detects collective harms not visible to a single agent (e.g., combined emissions, radio jamming pattern) |
| `AnomalyReportNeuron` | 2 / 1 | Reports misbehaving peers via `AgentAnomalySignal`; specialised `HarmFeedbackSignal` producer |
| `IsolationProtocolNeuron` | 1 / 1 | Implements Byzantine tolerance — isolates peers with multiple anomaly reports; quarantine is time-bounded, reuses `LoopCircuitBreakerNeuron` pattern |

### Layer 7 — homeostasis

| Class | Loop / Epoch | Role |
|---|---|---|
| `SwarmDensityNeuron` | 2 / 2 | Regulates local density — disperse if too dense, aggregate if too sparse |
| `BandwidthBudgetNeuron` | 2 / 1 | Tracks outbound message rate; rate-limits low-priority signals under congestion |
| `EnergyCoordinatorNeuron` | 2 / 1 | Specialises `EnergyNeuron`; routes own tasks to higher-battery peers when appropriate |

---

## 5. Role differentiation — emergent specialisation

Role differentiation falls out naturally from the existing architecture plus the curiosity module:

- Every agent starts with the same neuron set but randomly initialised weights and shuffled `NoveltySignal` history.
- `EmpowermentNeuron` favours actions that open more downstream options — the agent that stumbles into scout work gets empowerment reward for scouting, reinforcing it.
- `HebbianLearningNeuron` under differential reward → per-agent specialisation in weights without changing topology.
- `RoleAwarenessNeuron` observes the local distribution of roles — if the neighbourhood has too many scouts and no workers, the agent's intrinsic reward biases shift toward worker behaviour.

Result: a biologically-grounded mechanism for homeostatic role distribution without a central allocator. Matches observed dynamics in social insects.

---

## 6. Stigmergy — `NeuromodulatorSignal` becomes spatial

The framework's existing `NeuromodulatorSignal` is already a broadcast-to-region primitive. In swarm use, "region" becomes spatial:

- `PheromoneSignal` subclasses `NeuromodulatorSignal` with `locationGlobal` and `decayTick`.
- Agents deposit pheromones by emitting `PheromoneSignal(locationGlobal=self)`.
- Other agents within communication range receive it; those outside pick it up only when a third agent relays it — natural diffusion.
- `StigmergicTraceSignal` is the persistent-on-environment variant for agents with actual mark-depositing hardware (UV-visible markers, radio beacons).

`PheromoneKind` enum includes biologically-validated categories: `TRAIL` (routing), `ALARM` (dispersal), `RECRUITMENT` (aggregation), `TERRITORY` (claim / avoid), `FOOD` (reward signal).

---

## 7. Byzantine tolerance — the hard part honestly faced

Real swarms face faulty or compromised agents. The architecture handles this with:

- `AnomalyReportNeuron` — each agent rates peers on behavioural consistency. Reports are not accepted on the word of one witness.
- `IsolationProtocolNeuron` — quarantines a peer only after `k`-witness corroboration. `k` is configurable, minimum 3 for Byzantine f=1 tolerance on a small swarm.
- Time-bounded isolation — `LoopCircuitBreakerNeuron` pattern reused: isolation expires, peer can re-join; repeated isolation escalates the duration, never permanent locally but a global consensus decision can remove.
- Safety invariant preserved: `EthicalPriorityNeuron` ensures the agent itself cannot be manipulated into attacking peers based on falsified reports — hard-coded rule that any action-against-peer requires ≥ `k` independent witnesses.

Note the limits: this tolerates faulty agents, not adversarial insiders with full protocol knowledge. For deployments where adversarial insiders are in scope, pair with the cybersecurity use case's detection layer on top.

---

## 8. Collective harm discrimination

One agent emitting one order of magnitude below its own limit is safe; 100 agents each doing so can collectively exceed a regional limit. This needs `SwarmHarmGateNeuron` at the regional level:

- Each agent reports its projected effect (e.g., thermal, EM, chemical release) as a `ConsequenceSimulationSignal` that carries a spatial footprint.
- Regional aggregator sums projected effects over all agents in a region.
- If regional sum exceeds regional threshold → `SwarmAlertSignal` with `severity` proportional to excess → individual agents' `HarmGateNeuron` instances tighten their own thresholds via `HarmContextNeuron`.
- No single agent is unilaterally overriding the swarm; the aggregate signal propagates through normal channels and each agent retains local authority.

This is the collective-safety pattern: emergent safety via local signal flow, matching the framework's existing "safety is architectural, not post-hoc" principle.

---

## 9. Configuration

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
    quorum-witness-count: 3
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
    anomaly-threshold-votes: 3
    isolation-ticks: 600
    max-reputation-history: 1000
  collective-harm:
    regional-threshold-config: "regional-thresholds.yaml"
    aggregator-election: raft-lite
  curiosity:
    enabled: true
    role-differentiation: true
```

---

## 10. Validation criteria

Before deployment:

1. **Communication robustness.** Run with simulated packet loss from 0% → 60%; swarm behaviour should degrade gracefully, not cliff.
2. **Partition tolerance.** Split swarm into two non-communicating halves; each must operate sensibly; upon re-merge, state reconciles via consensus without conflict explosion.
3. **Byzantine drill.** Inject a misbehaving agent (wrong data, no-op, contrary behaviour); within `isolation-ticks` it should be isolated by the majority without collateral isolation of healthy agents.
4. **Scalability.** Run sizes 4 → 8 → 16 → 64 → 256; characterise where bandwidth, consensus latency, or throughput becomes the limit. Do not ship at a size beyond demonstrated stable operation.
5. **Collective harm test.** Configure 10 agents with individually-safe emission levels whose sum exceeds a regional threshold; `SwarmHarmGateNeuron` must trigger tightening and aggregate emission must fall below threshold within configured response window.
6. **Agent-loss resilience.** Kill 25% of agents at random mid-task; remaining agents must complete or gracefully abort, never deadlock or hang on missing peers.

---

## 11. Deployment topology

- **Per-agent** full jneopallium instance. Heterogeneous hardware acceptable — different agents can run different subsets of modules based on their role and compute budget.
- **Transport** — DDS (ROS 2 native), MQTT, ZeroMQ for TCP/IP networks. LoRa mesh for long-range low-bandwidth. Whichever the deployment uses, wrap it in `MeshRadioNeuron` with link-quality awareness.
- **Optional regional aggregator** — a beefier node running a jneopallium instance that subscribes to regional signals for `SwarmHarmGateNeuron` aggregation. The swarm must still function if the aggregator fails (fall back to pairwise pessimistic assumption).
- **Operator console** — consumes `SwarmAlertSignal`, `AgentAnomalySignal`, and task progress. Human can inject `TaskAnnouncementSignal` and emergency-stop the whole swarm but not micro-manage.
- **Simulation / digital twin mode** — run the same jneopallium code in a Gazebo / Isaac Sim loop; `SensorNeuron` inputs come from sim, `ActuatorNeuron` outputs go to sim. Essential for CI.

---

## 12. Out of scope

- Lethal autonomous weapons. Explicitly out of scope. The framework's harm discriminator plus `EthicalPriorityNeuron` hard constraints make it architecturally inappropriate for any use targeting humans.
- Centralised training with centralised execution. If a central planner is required, this framework is the wrong tool; use an MPC or ILP.
- Byzantine *adversarial* tolerance (insider attack). Use the cybersecurity use case's detection layer layered on top; this use case handles faulty-agent, not adversarial-agent, tolerance by itself.
- Fleet sizes beyond what has been measured in validation. Theoretical scaling ≠ operational stability.
- Use of affect module for emotional mimicry between agents. Not well-founded; out of scope.

---

## References

- Rakovskyi, D. (2024). *Framework for Natural Neuron Network Modeling: The Jneopallium Approach.* IJSR 13(7).
- Reynolds, C.W. (1987). Flocks, herds, and schools: A distributed behavioral model. *ACM SIGGRAPH.*
- Dorigo, M., Birattari, M., Stutzle, T. (2006). Ant colony optimization. *IEEE Computational Intelligence Magazine.*
- Camazine, S. et al. (2001). *Self-Organization in Biological Systems.* Princeton University Press.
- Brambilla, M., Ferrante, E., Birattari, M., Dorigo, M. (2013). Swarm robotics: a review from the swarm engineering perspective. *Swarm Intelligence* 7, 1–41.
- Castro, M., Liskov, B. (1999). Practical Byzantine Fault Tolerance. *OSDI.*
- Ongaro, D., Ousterhout, J. (2014). In search of an understandable consensus algorithm. *USENIX ATC* — Raft.
- Klyubin, A.S., Polani, D., Nehaniv, C.L. (2005). Empowerment: a universal agent-centric measure of control.
