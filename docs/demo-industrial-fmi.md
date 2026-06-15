# Industrial FMI Skid Demo

This demo adds a deterministic heated circulation and heat-exchanger skid around Jneopallium's existing industrial bridge stack.

```text
ThermalSkid FMI 2.0 Co-Simulation FMU
  -> Python plant gateway
     -> OPC UA process/control namespace
     -> MQTT IIoT telemetry topics
  -> Jneopallium industrial controller
     -> validation, cascade/PID control, oscillation supervision
     -> equipment-health advisory loop
     -> safety gate and audit outputs
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

OPC UA is the only autonomous actuator command path. The command nodes are:

| Actuator | OPC UA command node | Signal tag |
| --- | --- | --- |
| CV-101 cooling valve | `ns=2;s=Skid.CV101.PositionCMD` | `SKID.CV101.CMD` |
| P-101 pump speed | `ns=2;s=Skid.P101.SpeedSP` | `SKID.P101.SPEED.SP` |
| HTR-101 heater power | `ns=2;s=Skid.HTR101.PowerCMD` | `SKID.HTR101.POWER.CMD` |

MQTT is advisory-only. The Java `MqttBridgeConfig` constructor rejects `AUTONOMOUS`, and the demo advisory tags do not match actuator command tags.

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

The demo is simulation/HIL evidence. It is not a certified safety controller and does not replace PLC/SIS validation, hazard analysis, management of change, or site acceptance testing.
