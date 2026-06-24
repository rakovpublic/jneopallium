# Industrial FMI Skid Demo

This demo adds a deterministic heated circulation and heat-exchanger skid around Jneopallium's existing industrial bridge stack.

```text
PLC / PID / SIS
  -> deterministic millisecond control and hard safety
ThermalSkid FMI 2.0 Co-Simulation FMU
  -> Python plant gateway
     -> OPC UA process/control namespace
     -> MQTT IIoT telemetry topics
  -> Jneopallium Industrial Loop Guardian
     -> multi-loop supervisory diagnosis
     -> Java machine-health feature extraction, fault hypotheses, and domain-shift scoring
     -> oscillation, valve-stiction, and sensor-fault discrimination
     -> pump health, energy, economic basis, and maintenance planning
     -> bounded setpoint recommendations, safety gate, and audit outputs
```

## Run Commands

Linux:

```bash
scripts/demo-industrial-fmi/run_demo.sh all
scripts/demo-industrial-fmi/run_demo.sh normal
scripts/demo-industrial-fmi/run_demo.sh pump-wear
scripts/demo-industrial-fmi/run_demo.sh high-temperature-interlock
```

Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/demo-industrial-fmi/run_demo.ps1 all
powershell -ExecutionPolicy Bypass -File scripts/demo-industrial-fmi/run_demo.ps1 normal
```

Fast Python-only model verification:

```bash
python -m unittest discover -s scripts/demo-industrial-fmi/tests
```

Train and export the Industrial Loop Guardian maintenance and energy model:

```bash
python scripts/demo-industrial-fmi/train_loop_guardian_model.py \
  --reference-multiplier 1000 \
  --target-corpus-bytes 100gb \
  --max-corpus-bytes 100gb
```

The trained model package is written to
`worker/src/main/resources/model/industrial-loop-guardian/`. Production launch
details are in `docs/demo-industrial-fmi-production-deployment.md`.

Run the real Jneopallium `Entry local` machine-health workflow with a Java
`IInitInput` source and `configuration.runoncein=1`:

```powershell
$env:MAVEN_OPTS='-Xms32m -Xmx768m -XX:ReservedCodeCacheSize=96m -XX:+UseSerialGC'
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.1\plugins\maven\lib\maven3\bin\mvn.cmd' `
  -pl worker -DskipTests '-Dmaven.compiler.useIncrementalCompilation=false' `
  test-compile dependency:build-classpath '-Dmdep.outputFile=target\industrial-cp.txt'
$cp = "worker\target\test-classes;worker\target\classes;" + (Get-Content worker\target\industrial-cp.txt -Raw)
& 'C:\Program Files\Java\jdk-17.0.4.1\bin\java.exe' -Xms32m -Xmx768m -XX:+UseSerialGC `
  -cp $cp `
  com.rakovpublic.jneuropallium.worker.demo.industrialfmi.runtime.IndustrialLoopGuardianEntryLauncher `
  worker\target\industrial-loop-guardian-entry 80 1
```

The launcher writes `advisory-output.jsonl`, `input-audit.jsonl`, and
`advisory-summary.json`. The local replay input is a Java `IInitInput`; site
deployments swap it for the existing OPC UA, MQTT, FMI, or PLC4X `IInitInput`
bridges.

Generate training and production run reports after training and replay:

```bash
python scripts/demo-industrial-fmi/generate_run_reports.py
```

Build the FMU from source:

```bash
python -m pip install -r scripts/demo-industrial-fmi/plant/requirements.txt
python scripts/demo-industrial-fmi/plant/build_fmu.py
```

Run the protocol gateway directly after building the FMU:

```bash
python -m pip install -r scripts/demo-industrial-fmi/gateway/requirements.txt
python scripts/demo-industrial-fmi/gateway/plant_gateway.py \
  --scenario scripts/demo-industrial-fmi/config/scenarios/normal.yaml \
  --output-dir target/jneopallium-industrial-fmi/normal \
  --fmu scripts/demo-industrial-fmi/plant/generated/ThermalSkid.fmu \
  --mode REAL_TIME
```

## Safety Boundary

The demo positions Jneopallium above simple controls, not instead of them.
PLC/PID/SIS logic remains responsible for deterministic control and hard
interlocks. Jneopallium neurons own supervisory diagnosis, economic ranking,
maintenance planning, and bounded recommendations.

OPC UA is the only bounded actuator command path. The command nodes are:

| Actuator | OPC UA command node | Signal tag |
| --- | --- | --- |
| CV-101 cooling valve | `ns=2;s=Skid.CV101.PositionCMD` | `SKID.CV101.CMD` |
| P-101 pump speed | `ns=2;s=Skid.P101.SpeedSP` | `SKID.P101.SPEED.SP` |
| HTR-101 heater power | `ns=2;s=Skid.HTR101.PowerCMD` | `SKID.HTR101.POWER.CMD` |

MQTT is telemetry/advisory-only. The Java `MqttBridgeConfig` constructor rejects
`AUTONOMOUS`, and the demo advisory tags do not match actuator command tags.

Command priority is:

```text
hard interlock -> local fail-safe -> operator override -> safety mode
-> validation/quality -> clamp -> ramp limit -> diff suppression -> OPC UA write -> audit
```

Fail-safe defaults:

| Command | Fail-safe |
| --- | ---: |
| Cooling valve | 100 percent open |
| Pump speed | 30 percent |
| Heater power | 0 percent |

## FMI Variables

Inputs:

| Name | Unit / range |
| --- | --- |
| `coolingValveCmd` | 0-100 percent |
| `pumpSpeedCmd` | 0-100 percent |
| `heaterPowerCmd` | 0-100 percent |
| `processLoadFlow` | 0-1 |
| `ambientTemperature` | degrees C |
| `faultValveStuck` | Boolean |
| `faultPumpWear` | 0-1 |
| `faultTempSensorDrift` | degrees C |
| `faultThermalRunawayKw` | scenario fault heat input |

Outputs:

| Name |
| --- |
| `processTemperature` |
| `measuredTemperature` |
| `circulationFlow` |
| `suctionPressure` |
| `coolingValvePosition` |
| `pumpSpeedActual` |
| `pumpPowerKw` |
| `vibrationRms` |
| `bearingTemperature` |
| `highTemperatureInterlock` |
| `lowFlowInterlock` |
| `lowSuctionInterlock` |

The model includes tank thermal inertia, pump and valve lag, pump-flow and power curves, vibration and bearing-temperature wear effects, sensor drift/noise, and deterministic interlocks.

## OPC UA Namespace

The gateway exposes:

```text
ns=2;s=Skid.TIC101.PV
ns=2;s=Skid.TIC101.SP
ns=2;s=Skid.FIC101.PV
ns=2;s=Skid.P101.SpeedPV
ns=2;s=Skid.P101.SpeedSP
ns=2;s=Skid.CV101.PositionPV
ns=2;s=Skid.CV101.PositionCMD
ns=2;s=Skid.HTR101.PowerPV
ns=2;s=Skid.HTR101.PowerCMD
ns=2;s=Skid.SuctionPressure
ns=2;s=Skid.Interlock.HighTemperature
ns=2;s=Skid.Interlock.LowFlow
ns=2;s=Skid.Interlock.LowSuctionPressure
ns=2;s=Skid.Operator.ManualMode
ns=2;s=Skid.Operator.ManualValveCommand
ns=2;s=Skid.Operator.ManualPumpCommand
ns=2;s=Skid.Simulation.Time
ns=2;s=Skid.Simulation.Status
```

## MQTT Topics

Telemetry:

```text
jneopallium/demo/skid/P101/vibration
jneopallium/demo/skid/P101/bearing-temperature
jneopallium/demo/skid/P101/power-kw
jneopallium/demo/skid/environment/ambient-temperature
jneopallium/demo/skid/status
```

Advisories:

```text
jneopallium/demo/skid/advisory/recommended-pump-speed
jneopallium/demo/skid/advisory/maintenance-priority
jneopallium/demo/skid/advisory/predicted-bearing-risk
jneopallium/demo/skid/advisory/energy-mode
```

The Java runtime also supports `MachineHealthAdvisorySignal` for a
structured advisory payload containing `healthScore`,
`anomalyProbability`, `faultProbabilities`, `domainShiftScore`,
`uncertainty`, and evidence. This signal is read-only/advisory and is
kept outside the OPC UA actuator command path.

## Scenarios

Configured scenarios live in `scripts/demo-industrial-fmi/config/scenarios/`:

```text
normal
load-disturbance
oscillation
pump-wear
temperature-sensor-drift
mqtt-outage
opcua-outage
high-temperature-interlock
operator-override
```

Each run writes:

```text
target/jneopallium-industrial-fmi/<scenario>/
  manifest.json
  process_trace.csv
  controller_results.jsonl
  advisory_findings.jsonl
  model_advisory_findings.jsonl
  heuristic_advisory_findings.jsonl
  opcua_audit.jsonl
  mqtt_audit.jsonl
  alarms.jsonl
  interventions.jsonl
  metrics.json
  comparison.json
  gateway.log
  controller.log
```

## Acceptance Checks

The deterministic runner records evidence that:

- MQTT advisories are separate from actuator tags.
- High-temperature interlock writes cooling fail-safe and heater fail-safe.
- MQTT outage leaves fast-loop control availability at `1.0`.
- OPC UA outage applies local fail-safe values.
- Each scenario produces deterministic traces and baseline comparison metrics.
- Advisory JSON includes the owning neuron, recommendation code, economic basis,
  safety-envelope result, and the PLC/PID/SIS versus Jneopallium boundary.

The demo is simulation/HIL evidence. It is not a certified safety controller and does not replace PLC/SIS validation, hazard analysis, management of change, or site acceptance testing.
