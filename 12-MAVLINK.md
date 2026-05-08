# Bridge 12 — MAVLink (drones and small autonomous vehicles)

> **Prerequisite:** `00-FRAMEWORK.md`.

**Priority:** medium-high — it is the most direct path to a swarm-robotics demo. **Safety ceiling:** **SIM-ONLY** initially. Production deployments against a physical drone require a separate certification path and are out of scope for this bridge.

## 1. Domain context

[MAVLink](https://mavlink.io/) (Micro Air Vehicle Link) is the lightweight messaging protocol used by ArduPilot, PX4, QGroundControl, Mission Planner, and most open-source ground stations. It defines a packed binary format for telemetry (heartbeat, attitude, GPS, battery, IMU) and commands (`COMMAND_LONG`, mission upload, parameter set, mode change).

For Jneopallium this is the most accessible robotics path — the *simulators* (ArduPilot SITL, PX4 SITL, jMAVSim, Gazebo+PX4) are free, run on a developer laptop, and speak the same MAVLink dialect as physical drones. A bridge built and tested against SITL is structurally close to one that could fly hardware — but the gap is nonzero, and the certification gap is much larger than the technical one.

The repo's `swarm/` package already holds the natural target signals: `PeerObservationSignal`, `PeerStateSignal`, `FormationSignal`, `ConsensusProposalSignal`/`ConsensusVoteSignal`, `PheromoneSignal`, `TaskAnnouncementSignal`/`TaskBidSignal`/`TaskAssignmentSignal`. Per-vehicle proprioception reuses `embodiment/ProprioceptiveSignal` and `industrial/EfficiencySignal` (battery).

## 2. Maven dependency

```xml
<!-- DroneFleet's mavlink Java library — pure Java, supports MAVLink 1 + 2,
     covers common.xml, ardupilotmega.xml, uAvionix dialects -->
<dependency>
    <groupId>io.dronefleet.mavlink</groupId>
    <artifactId>mavlink</artifactId>
    <version>1.1.11</version>
</dependency>
```

Verify the latest version before merge.

## 3. Why sim-only initially

A drone in the air is a kinetic-energy hazard. Three structural reasons the bridge is sim-only at first:

1. **MAVLink command set includes mode changes, arm/disarm, takeoff/land, and goto.** Any of these reaching a physical vehicle from a software bridge is a safety event. The bridge's write surface is gated behind an explicit `simulatorOnly: true` flag whose default is `true` and whose flip to `false` requires a documented manual-edit, not a UI toggle.
2. **MAVLink does not have native authentication.** Most deployments rely on link-level isolation (telemetry radio at known frequency, USB serial). A bridge that flies a real vehicle has to add its own session/auth layer; this bridge does not.
3. **The autonomy supervisor pattern from the ROS 2 bridge applies here too.** Jneopallium emits *advice* — recommended waypoint, geofence warning, abort recommendation — to a separate supervisor node that decides whether to forward it to the autopilot.

The aggregator therefore writes to **advisory MAVLink message channels** by default — `STATUSTEXT`, `NAMED_VALUE_FLOAT`, custom dialect messages prefixed `JNEO_*`. The validator forbids generating any of `COMMAND_LONG`, `COMMAND_INT`, `MISSION_*`, `SET_MODE`, `MANUAL_CONTROL`, `RC_CHANNELS_OVERRIDE` unless `simulatorOnly: true`.

## 4. Architecture

```
ArduPilot SITL / PX4 SITL / hardware (sim only)
      │
      │ UDP 14550, TCP 5760, or serial
      ▼
┌────────────────────────┐
│ MavlinkClientService   │       parsed inbound:
│  • Connection (UDP/TCP │  HEARTBEAT, ATTITUDE, GLOBAL_POSITION_INT,
│    /serial)            │  GPS_RAW_INT, BATTERY_STATUS, SYS_STATUS,
│  • dialect detection   │  STATUSTEXT, RADIO_STATUS, MISSION_CURRENT
│  • per-system cache    │
│  • per-system MAV_ID   │       outbound (advisory):
│    routing             │  STATUSTEXT, NAMED_VALUE_FLOAT, JNEO_* custom
└─────┬──────────────┬───┘
      │              │
┌─────▼──────┐ ┌─────▼──────┐
│ MavTel     │ │ MavSwarm   │
│ Input      │ │ Input      │
│ → Proprio- │ │ → Peer-    │
│   ceptive  │ │   Obser-   │
│ → Efficien │ │   vation,  │
│   cy (bat) │ │   PeerState│
└────────────┘ └────────────┘
                ▼
[Pipeline → MavlinkAdvisoryOutputAggregator]
                ▼
publish JNEO_* / STATUSTEXT only,
no COMMAND_LONG outside simulator
```

A single bridge instance can speak to multiple MAV systems (each identified by `system_id`/`component_id`) — so a five-drone SITL demo is one bridge with five `system_id`s in the cache.

## 5. Signal mapping

All target signals already exist; this is purely a mapper bridge.

| MAVLink message | Bridge action | Jneopallium signal |
|---|---|---|
| `HEARTBEAT` | refresh per-system liveness; if missing >2 s, mark stale | bridge metadata; one `AlarmSignal(MEDIUM, "PEER_OFFLINE")` after timeout |
| `GLOBAL_POSITION_INT` (lat/lon/alt/heading) | per-system position cache | `PeerObservationSignal` (other MAVs) **or** `ProprioceptiveSignal` (own MAV) per binding |
| `ATTITUDE` (roll/pitch/yaw + rates) | per-system pose | `ProprioceptiveSignal` |
| `BATTERY_STATUS` (voltage, current, remaining %) | per-system energy | `EfficiencySignal` |
| `SYS_STATUS` (sensor health flags, drop rate) | per-system health | `AlarmSignal` if any health flag set |
| `STATUSTEXT` (severity + text) | passthrough | `AlarmSignal` with severity mapped from MAVLink severity |
| `MISSION_CURRENT` | bridge metadata | none |
| `RADIO_STATUS` | link quality | `AnomalyScoreSignal` if RSSI/noise crosses thresholds |
| GCS-level swarm consensus on a custom dialect | per-task | `ConsensusProposalSignal` / `ConsensusVoteSignal` |

Egress (advisory):

| Jneopallium signal | MAVLink output | Notes |
|---|---|---|
| `FormationSignal` | `JNEO_FORMATION` (custom dialect) | Consumed by the supervisor or GCS, not the autopilot |
| `HarmVetoSignal` | `STATUSTEXT(severity=CRITICAL)` | Operator visibility |
| `TaskAnnouncementSignal` | `JNEO_TASK_ANNOUNCEMENT` | Swarm-level coordination message |
| `TransparencyLogSignal` | dedicated log channel (file or Kafka via OTel/Kafka bridge) | NOT MAVLink — keep audit out of the flight bus |

## 6. Configuration

```yaml
connections:
  - id: "FLEET-A"
    transport: "UDP"
    bindAddress: "0.0.0.0"
    bindPort: 14550
    expectedSystems: [1, 2, 3, 4, 5]
  - id: "FLEET-B-DEV"
    transport: "TCP"
    host: "127.0.0.1"
    port: 5760
    expectedSystems: [10]

simulatorOnly: true                     # MUST be true unless deployed against
                                        # a certified hardware setup; flag is
                                        # checked by the config validator.

reads:
  # Per-system telemetry
  - bindingId: "DRONE-1-POS"
    connectionId: "FLEET-A"
    systemId: 1
    componentId: 1
    messageType: "GLOBAL_POSITION_INT"
    targetSignal: "PROPRIOCEPTIVE"      # own-MAV mode
    signalTag: "DRONE.1.POS"

  # Peer observation: drone 1 watches drone 2
  - bindingId: "DRONE-1-SEES-2"
    connectionId: "FLEET-A"
    systemId: 2
    messageType: "GLOBAL_POSITION_INT"
    targetSignal: "PEER_OBSERVATION"
    signalTag: "DRONE.1.PEER.2.POS"

  - bindingId: "DRONE-1-BATT"
    connectionId: "FLEET-A"
    systemId: 1
    messageType: "BATTERY_STATUS"
    targetSignal: "EFFICIENCY"
    signalTag: "DRONE.1.BATT"

events:
  - bindingId: "STATUS-TEXTS"
    connectionId: "FLEET-A"
    messageType: "STATUSTEXT"
    targetSignal: "ALARM"
    signalTagPrefix: "DRONE.STATUS"

writes:
  - bindingId: "FORMATION-ADV"
    connectionId: "FLEET-A"
    messageType: "JNEO_FORMATION"        # custom dialect — see §7
    signalTag: "FLEET.FORMATION.ADV"

audit:
  localAuditFile: "/var/log/jneopallium/mavlink-audit.jsonl"

perTagSafetyMode:
  FORMATION-ADV: ADVISORY    # AUTONOMOUS rejected unless simulatorOnly=true
                             # AND deployment-runbook attestation present
```

The config validator rejects:
* Any write `messageType` in `{COMMAND_LONG, COMMAND_INT, SET_MODE, MISSION_*, MANUAL_CONTROL, RC_CHANNELS_OVERRIDE}` unless `simulatorOnly: true`.
* Any binding for a `systemId` not declared in `connections[*].expectedSystems` (catches typos that would silently consume cross-talk).

## 7. Custom dialect

Define a small `jneo.xml` MAVLink dialect with these messages:
* `JNEO_FORMATION` — recommended formation geometry (target offsets per peer).
* `JNEO_TASK_ANNOUNCEMENT` — task id + parameters for swarm consensus.
* `JNEO_HARM_VETO` — explicit veto with reason code.

Generate Java sources via `dronefleet/mavlink` codegen at build time. Commit the generated sources to keep the build hermetic.

## 8. Package layout

```
worker/src/main/java/com/rakovpublic/jneuropallium/worker/bridge/mavlink/
├── MavlinkBridgeConfig.java
├── MavlinkBridgeConfigLoader.java
├── MavlinkConnectionBinding.java
├── MavlinkMessageBinding.java
├── MavlinkSignalMapper.java
├── MavlinkClientService.java          (UDP/TCP/serial, multi-system routing)
├── MavlinkTelemetryInput.java         (GLOBAL_POSITION_INT, ATTITUDE, BATTERY_STATUS, SYS_STATUS)
├── MavlinkSwarmInput.java             (HEARTBEAT-driven peer table, custom JNEO_*)
├── MavlinkEventInput.java             (STATUSTEXT)
└── MavlinkAdvisoryOutputAggregator.java
```

## 9. Phase plan

| Phase | Goal |
|-------|------|
| 1 | Read-only against ArduPilot SITL on UDP 14550. One vehicle, full telemetry → typed signals. |
| 2 | Multi-system swarm: 5× SITL instances with distinct `system_id`s; PeerObservation / PeerState wiring. |
| 3 | Advisory writes via custom dialect. Validator enforces simulator-only. |
| 4 | **Not pursued in this bridge.** Hardware deployment is a separate, certified bridge. |

## 10. Bridge-specific scenarios

| # | Scenario | Setup | Expected |
|---|----------|-------|----------|
| **S7** | SITL telemetry | `sim_vehicle.py -v ArduCopter --console`; bridge bound to UDP 14550 | `ProprioceptiveSignal` for system 1 emitted within 2 s; updates at the SITL telemetry rate |
| **S8** | Multi-system swarm | 3× SITL with system_ids 1/2/3 | Bridge produces `PeerObservationSignal` cross-products as configured |
| **S9** | Heartbeat loss | Kill one SITL instance | After 2 s with no `HEARTBEAT`, bridge emits `AlarmSignal(MEDIUM, PEER_OFFLINE)` for that system |
| **S10** | Forbidden write rejected | YAML has `messageType: COMMAND_LONG`, `simulatorOnly: false` | `MavlinkBridgeConfigLoader.load()` throws |
| **S11** | Battery low advisory | SITL battery at 20 % | Bridge emits `EfficiencySignal`; downstream planning emits a `HarmVetoSignal` → bridge sends `JNEO_HARM_VETO` (in sim mode) |
| **S12** | Custom dialect roundtrip | Bridge sends `JNEO_FORMATION`; a test MAVLink listener decodes it | Decoded message matches input fields |

## 11. Risks

| # | Risk | Mitigation |
|---|------|------------|
| R1 | Accidental cross-talk between SITL and a nearby real drone on shared UDP | Bridge requires explicit `expectedSystems` whitelist; messages from unknown systems are dropped with a single audit per minute. |
| R2 | MAVLink 1 vs 2 confusion | Library auto-detects per stream; bridge logs the negotiated version per connection. |
| R3 | Telemetry storm DOS | Per-binding rate cap (Hz); above cap, decimate with audit entry. |
| R4 | Custom dialect drift | Pin dialect XML; codegen output is committed, not regenerated at build time. |

## 12. References

* MAVLink — `https://mavlink.io/`.
* dronefleet/mavlink Java library — `https://github.com/dronefleet/mavlink`.
* ArduPilot SITL — `https://ardupilot.org/dev/docs/sitl-with-mavproxy.html`.
* PX4 SITL — `https://docs.px4.io/main/en/simulation/`.
