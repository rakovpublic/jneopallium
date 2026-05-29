# Demo 04 — Drone geofence & battery supervisor (MAVLink, SIM-ONLY)

> Bridge: **MAVLink** ([doc](../mavlink-bridge.md)) ·
> Core: **autonomous-AI harm discriminator**
> ([autonomous agent demo](demo-08-autonomous-agent-sandbox.md),
> [`ai.neurons.harm`]) · Safety ceiling: **SIM-ONLY** (the config loader refuses
> autonomous writes on a non-SITL link) · External system: **ArduPilot SITL**
> (or PX4 SITL) reachable over UDP.

A safety-supervisor demo flown entirely in simulation. Vehicle telemetry feeds a
consequence model that projects the next manoeuvre against a geofence and a
battery-endurance budget; the harm gate vetoes unsafe trajectories and the
network emits an **advisory** Return-to-Launch / Land recommendation. Nothing
arms or actuates real hardware.

## Scenario

A quadcopter flies a mission in ArduPilot SITL. The bridge subscribes to
`GLOBAL_POSITION_INT`, `SYS_STATUS` (battery), `HEARTBEAT` (flight mode), and
`STATUSTEXT`. A supervisory sub-net continuously simulates the projected position
a few seconds ahead and the energy required to return home. If the projection
breaches the geofence polygon, or if state-of-charge falls below the
return-home reserve, the harm gate raises a veto and the advisory aggregator
recommends `RTL` (or `LAND` when return is no longer feasible). A human operator
(or, in SITL, the mission script) acts on the advisory.

## What it demonstrates

| Feature | Where |
|---|---|
| MAVLink telemetry → typed measurements | `MavlinkTelemetryInput`, `MavlinkSignalMapper` |
| MAVLink `STATUSTEXT`/failsafe events → alarms | `MavlinkEventInput` |
| Consequence model: simulate the projected action before acting | `ConsequenceModelNeuron` / `IConsequenceModelNeuron` |
| Multi-dimension harm evaluation + veto | `HarmEvaluationNeuron`, `HarmGateNeuron`, `HarmVetoSignal` |
| Ethical-priority ordering of competing options | `EthicalPriorityNeuron` |
| Action selection among RTL / LAND / CONTINUE | `ActionSelectionNeuron`, `MotorCommandSignal` |
| **SIM-ONLY ceiling** enforced at config load | `MavlinkBridgeConfigLoader` |
| Advisory-only command emission + audit | `MavlinkAdvisoryOutputAggregator`, `MavlinkAuditOutput` |

## Architecture / data flow

```
 ArduPilot SITL  (udp:127.0.0.1:14550)
   GLOBAL_POSITION_INT  SYS_STATUS  HEARTBEAT  STATUSTEXT
        │ MAVLink
        ▼
  ┌──────────────────────────────────────────────┐
  │ MavlinkClientService (MavlinkTransport)        │
  └───┬───────────────────────────┬───────────────┘
      ▼                           ▼
 MavlinkTelemetryInput        MavlinkEventInput
   → MeasurementSignal          → AlarmSignal (FAILSAFE_*, GEOFENCE_*)
   (lat/lon/alt, battery %)
      │                           │
      ▼                           ▼
  ┌──────────────────────────────────────────────────────────┐
  │ Supervisor sub-net (ai.neurons.*):                        │
  │  ConsequenceModel → ConsequenceSimulationSignal           │
  │     (project position +Δt; energy-to-home)                │
  │  HarmEvaluation → HarmAssessmentSignal                    │
  │     dims: physicalIntegrity, resource, autonomy, …        │
  │  EthicalPriority → rank {CONTINUE, RTL, LAND}             │
  │  HarmGate → HarmVetoSignal (block CONTINUE if unsafe)     │
  │  ActionSelection → MotorCommandSignal(advisory verb)      │
  └───────────────────────────┬──────────────────────────────┘
                              ▼ List<IResult>
  ┌──────────────────────────────────────────────────────────┐
  │ MavlinkAdvisoryOutputAggregator (advisory namespace only) │
  │  emits advisory RTL/LAND recommendation + audit;          │
  │  loader forbids AUTONOMOUS on a non-SITL endpoint         │
  └───────────────────────────┬──────────────────────────────┘
                              ▼
            GCS / mission script (human-in-the-loop)  +  MavlinkAuditOutput
```

## Components used

* **Signals**: telemetry → `MeasurementSignal`; MAVLink events → `AlarmSignal`;
  AI core (`ai.signals.fast`): `ConsequenceQuerySignal`,
  `ConsequenceSimulationSignal`, `HarmAssessmentSignal`, `HarmVetoSignal`,
  `MotorCommandSignal`, `TransparencyLogSignal`.
* **Neurons** (`ai.neurons.harm`, `ai.neurons.action`): `ConsequenceModelNeuron`,
  `HarmEvaluationNeuron`, `EthicalPriorityNeuron`, `HarmGateNeuron`,
  `HarmLearningNeuron` (slow-loop refinement), `ActionSelectionNeuron`.
* **Bridge** (`worker.bridge.mavlink`): `MavlinkBridgeConfigLoader`,
  `MavlinkClientService`, `MavlinkTransport`, `MavlinkSignalMapper`,
  `MavlinkTelemetryInput`, `MavlinkEventInput`, `MavlinkAdvisoryOutputAggregator`,
  `MavlinkAuditOutput`. (`MavlinkSwarmInput` is available for multi-vehicle
  extensions — see [`../modules/swarm.md`](../modules/swarm.md).)

## Configuration

`/tmp/demo04-drone.yaml`:

```yaml
connection:
  endpoint: "udp:127.0.0.1:14550"
  systemId: 255                 # GCS id
  componentId: 190
  heartbeatTimeout: "PT3S"
  simulationOnly: true          # demo: refuses real-link autonomous writes

reads:
  - bindingId: "POS"
    message: "GLOBAL_POSITION_INT"
    signalTag: "UAV.POS"
  - bindingId: "BATT"
    message: "SYS_STATUS"
    field: "battery_remaining"
    signalTag: "UAV.BATT.SOC"
  - bindingId: "MODE"
    message: "HEARTBEAT"
    field: "custom_mode"
    signalTag: "UAV.MODE"

geofence:
  polygon:                      # simple square fence (lat,lon)
    - [ -35.3627, 149.1650 ]
    - [ -35.3627, 149.1670 ]
    - [ -35.3647, 149.1670 ]
    - [ -35.3647, 149.1650 ]
  maxAltitudeM: 120.0
battery:
  returnReserveFraction: 0.25   # SOC below which RTL is advised
  landReserveFraction:   0.10   # SOC below which LAND is advised

advisory:
  topic: "UAV.ADVISORY.CMD"     # advisory verb (RTL / LAND / CONTINUE)
audit:
  localAuditFile: "/tmp/jneopallium-demo04-audit.jsonl"
perTagSafetyMode:
  UAV.ADVISORY.CMD: ADVISORY    # AUTONOMOUS rejected unless simulationOnly + SITL
tickInterval: "PT0.2S"
```

## Run procedure

1. **Start ArduPilot SITL** (copter) and confirm it streams to UDP 14550:

   ```sh
   sim_vehicle.py -v ArduCopter --console --map \
     --out=udp:127.0.0.1:14550
   ```

   (PX4 SITL works equally well; point `endpoint` at its MAVLink UDP port.)

2. **Build and wire the bridge + supervisor:**

   ```java
   var cfg    = MavlinkBridgeConfigLoader.load(Path.of("/tmp/demo04-drone.yaml"));
   var audit  = new MavlinkAuditOutput(Path.of(cfg.audit().localAuditFile()));
   var mapper = new MavlinkSignalMapper(cfg);
   var svc    = new MavlinkClientService(cfg, mapper, audit);
   svc.start();

   var teleIn = new MavlinkTelemetryInput("mav-tele", svc, List.of("POS","BATT","MODE"));
   var evtIn  = new MavlinkEventInput("mav-evt", svc);
   var agg    = new MavlinkAdvisoryOutputAggregator(svc, audit);
   // build: consequenceModel → harmEvaluation → ethicalPriority →
   //        harmGate → actionSelection
   ```

3. **Fly inside the fence.** Take off and loiter well within the polygon and
   above the battery reserve. The supervisor should select `CONTINUE`; no veto;
   audit lines show the safe verdict.

4. **Approach the geofence.** Command a waypoint near/over the fence edge. The
   consequence model projects the position breaching the polygon within the
   look-ahead window; `HarmEvaluationNeuron` raises `physicalIntegrity` /
   `autonomy` harm above threshold; `HarmGateNeuron` emits `HarmVetoSignal` for
   `CONTINUE`; `ActionSelectionNeuron` advises `RTL`. The advisory aggregator
   publishes the `RTL` recommendation and a `TransparencyLogSignal` records the
   reason.

5. **Drain the battery.** Let SOC fall below `returnReserveFraction` → advisory
   `RTL`; below `landReserveFraction` (return no longer feasible from the
   projected position) → advisory `LAND`.

6. **Prove the ceiling.** Set `simulationOnly: false` (or point at a non-SITL
   endpoint) and attempt `perTagSafetyMode: AUTONOMOUS` — the loader must reject
   it. SIM-ONLY autonomy is allowed only against SITL.

## Acceptance

* Inside-fence, above-reserve flight selects `CONTINUE` with no veto.
* A trajectory projected to breach the fence is vetoed *before* the breach and
  produces an advisory `RTL` plus a transparency-log entry citing the dimension
  that tripped.
* SOC crossing the return/land reserves produces advisory `RTL` then `LAND`.
* `AUTONOMOUS` is rejected on any non-SITL / non-simulation configuration.
* Audit JSONL conforms to `00-FRAMEWORK §4`; each advisory carries proposed verb,
  effective verb, and reason.

## Safety / regulatory posture

This bridge is **SIM-ONLY** at the structural ceiling: autonomous command write
is permitted only against a simulator, and the harm gate is a *consequence
model* (it simulates and vetoes), not an output filter — matching the autonomous
architecture's design (see
[demo 08](demo-08-autonomous-agent-sandbox.md) and the README "Autonomous AI
Architecture"). Real-vehicle flight is operator-commanded; the framework advises.
The harm gate's hard constraints (e.g. never advise `CONTINUE` into a projected
collision) are structurally inviolable. See [`../mavlink-bridge.md`](../mavlink-bridge.md).
