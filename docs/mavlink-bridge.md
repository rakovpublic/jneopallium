# MAVLink bridge

> Companion to [`12-MAVLINK.md`](../12-MAVLINK.md) (the spec) and
> [`00-FRAMEWORK.md`](../00-FRAMEWORK.md) (the universal contract). This file
> documents the MAVLink bridge's domain context, YAML schema, manual demo
> procedure, and regulatory posture — i.e. the §10 Definition-of-Done items
> that don't belong in the spec itself.

## Domain context

[MAVLink](https://mavlink.io/) (Micro Air Vehicle Link) is the lightweight
binary messaging protocol used by ArduPilot, PX4, QGroundControl, Mission
Planner and most open-source ground stations. The bridge wraps the pure-Java
[`dronefleet/mavlink`](https://github.com/dronefleet/mavlink) codec, which
covers MAVLink 1 + 2, the `common.xml` dialect and the `ardupilotmega.xml`
extensions, and surfaces those messages onto the Jneopallium signal bus.

The bridge is **SIM-ONLY** by construction (12-MAVLINK.md §3): it subscribes
to drone telemetry (`HEARTBEAT`, `GLOBAL_POSITION_INT`, `ATTITUDE`,
`BATTERY_STATUS`, `SYS_STATUS`, `STATUSTEXT`, `RADIO_STATUS`,
`MISSION_CURRENT`), emits typed `ProprioceptiveSignal` /
`PeerObservationSignal` / `EfficiencySignal` / `AlarmSignal` /
`AnomalyScoreSignal` instances, and publishes neuron-derived
`HarmVetoSignal` / `FormationSignal` / `TaskAnnouncementSignal` decisions as
**advisory** MAVLink messages (`STATUSTEXT`, `NAMED_VALUE_FLOAT`, custom
`JNEO_*` dialect) consumed by an external supervisor or ground station — never
as `COMMAND_LONG`, `SET_MODE`, `MANUAL_CONTROL`, `RC_CHANNELS_OVERRIDE`,
`MISSION_*`, `COMMAND_INT` directly to a flying autopilot.

The forbidden-message-type rule is enforced both at config load (in
`MavlinkBridgeConfig`) and at runtime (in `MavlinkClientService.send`);
per-tag `AUTONOMOUS` promotion is rejected unless `simulatorOnly: true` is
set, which models the simulator-with-watchdog escape from §3. A
non-whitelisted `system_id` (a stranger drone on the shared UDP segment) is
dropped with one audit per minute (§11 R1).

## Components

```
worker/src/main/java/com/rakovpublic/jneuropallium/worker/bridge/mavlink/
├── MavlinkBridgeConfig.java               ← YAML record (immutable, COMMAND_LONG rejected)
├── MavlinkBridgeConfigLoader.java         ← Jackson YAML loader, FAIL_ON_UNKNOWN
├── MavlinkConnectionBinding.java          ← per-connection routing record
├── MavlinkMessageBinding.java             ← read/event/write binding (BridgeBinding)
├── MavlinkSignalMapper.java               ← MAVLink payload ↔ typed signal (pure)
├── MavlinkTransport.java                  ← test-seam interface
├── MavlinkClientService.java              ← lifecycle, multi-system routing, heartbeat watchdog
├── MavlinkTelemetryInput.java             ← IInitInput → ProprioceptiveSignal/EfficiencySignal
├── MavlinkSwarmInput.java                 ← IInitInput → PeerObservationSignal
├── MavlinkEventInput.java                 ← IInitInput → AlarmSignal/AnomalyScoreSignal
├── MavlinkAuditOutput.java                ← local JSONL audit (no in-band mirror — §5)
├── MavlinkAdvisoryOutputAggregator.java   ← IOutputAggregator (STATUSTEXT/NAMED_VALUE_FLOAT only)
└── package-info.java
```

## YAML schema

Refer to 12-MAVLINK.md §6 for the canonical example. Key points:

* `connections` is a list — one MAVLink connection per entry. `transport`
  is `UDP` (bind on `bindAddress` + `bindPort`, e.g. `0.0.0.0:14550` for
  ArduPilot SITL) or `TCP` (connect to `host:port`, e.g. `127.0.0.1:5760`
  for PX4 SITL). Serial is hardware-only and out of scope.
* `expectedSystems` whitelists the `system_id`s that can talk on the
  connection. Any other system is dropped silently with a single audit per
  minute. **Always set this** when sharing a UDP segment with anything
  else (§11 R1).
* `simulatorOnly: false` (the production default) forbids `writes` whose
  `messageType` names an actuating command. Setting `simulatorOnly: true`
  unlocks them — only do that when (a) the connection points at a SITL
  instance and (b) the operator runbook documents the watchdog node.
* Each `read` binding maps one `(systemId, messageType)` tuple to one
  signal class. The class is inferred from `messageType` if `targetSignal`
  is not set:
  - `GLOBAL_POSITION_INT`, `ATTITUDE`, `GPS_RAW_INT` → `ProprioceptiveSignal`
    (own-MAV) or `PeerObservationSignal` (when `targetSignal:
    PEER_OBSERVATION` and `messageType: GLOBAL_POSITION_INT`)
  - `BATTERY_STATUS` → `EfficiencySignal` (`batteryRemaining` / 100)
  - `SYS_STATUS` → `AlarmSignal` when an enabled sensor is unhealthy
  - `STATUSTEXT` → `AlarmSignal` (severity → `AlarmPriority`)
  - `RADIO_STATUS` → `AnomalyScoreSignal` when RSSI < 30 or noise > 80
* `events` are message types whose binding is system-agnostic: a single
  `STATUSTEXT` binding fires for every drone on the connection. The
  resulting signal's tag is `<signalTagPrefix>.<systemId>`.
* `decimateBy` on a read binding drops all but every Nth message.
* Each `write` binding clamps to `[minClampValue, maxClampValue]` and
  rate-limits to `rampRateMaxPerSec`. The bridge encodes outbound
  advisory traffic as `STATUSTEXT` for `HarmVetoSignal` / `FormationSignal`
  / `TaskAnnouncementSignal`. Once the `jneo.xml` dialect is generated and
  committed (§7), swap the encoder helpers in
  `MavlinkAdvisoryOutputAggregator.MavlinkAdvisoryEncoder` for the typed
  custom-dialect payloads.

## Heartbeat liveness

`MavlinkClientService.checkHeartbeats(nowMs)` should be called once per tick
by the host loop. For every `(connectionId, systemId)` from which a
`HEARTBEAT` has ever arrived, if more than 2s elapses without a fresh one,
the bridge emits a `PEER_OFFLINE` `AlarmSignal` (priority `HIGH`) on the
advisory event channel and remembers it so the alarm is not duplicated until
the next online → offline transition.

## Manual demo (ArduPilot SITL)

Phase 1 acceptance test for the bridge follows §10 S7. Outside of
unit/integration tests, the manual demo is:

```bash
# 1. Bring up ArduPilot SITL bound to UDP 14550.
sim_vehicle.py -v ArduCopter --console --map

# 2. Point the bridge at the SITL endpoint and bind GLOBAL_POSITION_INT.
cat <<EOF > /tmp/mavlink-bridge.yaml
connections:
  - id: SITL
    transport: UDP
    bindAddress: "0.0.0.0"
    bindPort: 14550
    expectedSystems: [1]
simulatorOnly: true
reads:
  - bindingId: DRONE-1-POS
    connectionId: SITL
    systemId: 1
    componentId: 1
    messageType: GLOBAL_POSITION_INT
    targetSignal: PROPRIOCEPTIVE
    signalTag: DRONE.1.POS
  - bindingId: DRONE-1-ATT
    connectionId: SITL
    systemId: 1
    messageType: ATTITUDE
    signalTag: DRONE.1.ATT
  - bindingId: DRONE-1-BATT
    connectionId: SITL
    systemId: 1
    messageType: BATTERY_STATUS
    signalTag: DRONE.1.BATT
events:
  - bindingId: STATUS-TEXTS
    connectionId: SITL
    messageType: STATUSTEXT
    targetSignal: ALARM
    signalTagPrefix: DRONE.STATUS
audit:
  localAuditFile: /tmp/jneopallium/mavlink-audit.jsonl
EOF

# 3. Run a small Java program that loads the config, builds
#    MavlinkClientService + MavlinkTelemetryInput/MavlinkEventInput, and
#    prints the signals it drains each tick. After ~2s the cache should
#    contain ProprioceptiveSignal / EfficiencySignal instances reflecting
#    SITL telemetry; takeoff in the SITL console produces ATTITUDE and
#    GLOBAL_POSITION_INT updates at the configured telemetry rate.
```

A multi-system swarm demo (§10 S8) follows the same recipe with N copies of
SITL on different ports / `system_id`s and `expectedSystems: [1, 2, 3, 4, 5]`.
For PX4 SITL substitute the `transport: TCP` form and point `host`/`port` at
`127.0.0.1:5760`.

## Regulatory posture

The bridge ceiling is `SIM-ONLY` (12-MAVLINK.md §3, §11.12). A drone in the
air is a kinetic-energy hazard: the bridge does **not** emit
`COMMAND_LONG`, `COMMAND_INT`, `SET_MODE`, `MANUAL_CONTROL`,
`RC_CHANNELS_OVERRIDE`, `MISSION_ITEM`, `MISSION_ITEM_INT`, `MISSION_COUNT`,
`MISSION_CLEAR_ALL`, `MISSION_SET_CURRENT` or `MISSION_WRITE_PARTIAL_LIST`
in any production environment. The intended steady state is:

* Bridge subscribes to telemetry → produces typed signals.
* Jneopallium swarm/planning module reasons over them.
* Bridge publishes `STATUSTEXT` and `NAMED_VALUE_FLOAT` (and, once
  generated, `JNEO_FORMATION` / `JNEO_TASK_ANNOUNCEMENT` / `JNEO_HARM_VETO`)
  to a ground station or autonomy supervisor that decides whether to forward
  the advice to the autopilot.

`TransparencyLogSignal` is intentionally never written to MAVLink (§5):
audit records belong on a dedicated channel (the JSONL file plus, if
configured, a Kafka topic via the OpenTelemetry/Kafka bridges) — not on
the flight bus. Hardware deployment requires a separate, certified bridge;
that work is explicitly out of scope here.
