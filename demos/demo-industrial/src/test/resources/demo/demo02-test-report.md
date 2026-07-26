# Demo-02 Test Report — Pump-Fleet Predictive Maintenance (MQTT + Sparkplug B)

**Date:** 2026-05-29
**Branch:** `claude/kind-einstein-UPmFK`
**Spec:** [`demo-02-pump-fleet-predictive-maintenance.md`](../../../../../../../../demo-02-pump-fleet-predictive-maintenance.md)
**Bridge ceiling:** **ADVISORY** (structural)

---

## Summary

| Suite | Tests | Passed | Failed | Skipped |
|-------|------:|-------:|-------:|--------:|
| `Demo02PumpFleetPredictiveMaintenanceTest` | 8 | 8 | 0 | 0 |
| `Demo02ConfigYamlTest`                     | 3 | 3 | 0 | 0 |
| **Total (new demo-02 tests)**              | **11** | **11** | **0** | **0** |

All 11 tests pass as part of the full worker suite (850 tests, 0 failures).

---

## How to Run

```bash
# run only the demo-02 tests
mvn -pl worker test -Dtest=Demo02PumpFleetPredictiveMaintenanceTest,Demo02ConfigYamlTest

# full worker suite
mvn -pl worker test

# narrated walk-through (writes audit JSONL to /tmp/jneopallium-demo02-audit.jsonl)
mvn -q -pl worker compile dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt
java -cp "worker/target/classes:$(cat /tmp/cp.txt)" \
  com.rakovpublic.jneuropallium.worker.demo.industrial.Demo02PumpFleetPredictiveMaintenance
```

---

## Test-by-Test Results

### T-1 `rul_decaysMonotonicallyOnRampedPump_andStaysHigherOnFlatPump`

**Acceptance bullet:** *RUL for the ramped pump decreases monotonically with the vibration trend; flat pumps hold steady RUL.*

| Step | Observation | Result |
|------|-------------|--------|
| 1500 ticks, P01 vibration ramps 0.005 mm/s/s, others flat at 1.0 mm/s | `rulEnd(P01) < rulStart(P01)` | PASS |
| After ramp                                                              | `rulEnd(P01) < rulEnd(P10)`   | PASS |
| Flat pump P10 after 1500 ticks                                          | `rulEnd(P10) > 2000h horizon` | PASS |

**Observed values:** `rul(P01) ≈ 1631h`, `rul(P10) ≈ 7260h`.

---

### T-2 `maintenanceWindow_proposedAtHorizonCrossing_publishedToAdvisoryTopic`

**Acceptance bullet:** *A maintenance window is proposed only when RUL crosses the horizon, scheduled before predicted EOL, and is published to the advisory namespace — never to a live DCMD actuator topic.*

| Step | Observation | Result |
|------|-------------|--------|
| 1500 ticks with P01 ramping                            | `proposalsFor(P01) == 1`                                | PASS |
| Flat pumps                                              | `proposalsFor(P10) == 0`                                | PASS |
| Advisory topic for P01                                  | exactly 1 publish to `spBv1.0/Plant1/DCMD/Edge-Pump/P01/advisory/maint_window` | PASS |
| Every publish topic                                     | contains `/advisory/` (no DCMD live actuator topics)    | PASS |
| Audit `APPLIED` line for P01                            | `0 < proposed ≤ 8760` hours-ahead                       | PASS |

**Observed proposed value:** `~1892 h` ahead — i.e., schedule the work order ~78 days before the predicted bearing failure (with the 100 h `MIN_LEAD_TIME` head-room).

---

### T-3 `autonomousPromotion_rejectedByLoader`

**Acceptance bullet:** *The config loader rejects any per-tag `AUTONOMOUS`.*

| Step | Observation | Result |
|------|-------------|--------|
| Build `MqttBridgeConfig` with `PUMP-MAINT-ADV-P01: AUTONOMOUS` | throws `IllegalArgumentException` | PASS |
| Error message                                                  | mentions the ADVISORY ceiling   | PASS |

The `MqttBridgeConfig` record's compact constructor enforces the rule, so any caller — YAML loader or in-code factory — gets the same structural rejection.

---

### T-4 `alarmStorms_areSuppressedByAggregator`

**Acceptance bullet:** *Alarm storms are suppressed/rate-limited (ISA-18.2) rather than flooding.*

| Step | Observation | Result |
|------|-------------|--------|
| First alarm `(tag=PUMP.P01, code=HIGH_VIB)`               | aggregator forwards it          | PASS |
| 10 repeated alarms within 600-tick suppression window     | every one is dropped            | PASS |
| Distinct alarm `(tag=PUMP.P01, code=DEVICE_OFFLINE)`      | passes through (per-condition suppression) | PASS |

`AlarmAggregationNeuron` keys suppression on `(tag, conditionCode)` so genuinely new conditions are never suppressed by an older standing alarm.

---

### T-5 `ddeath_emitsDeviceOfflineAlarm`

**Acceptance bullet:** *Device-offline alarm: send an `NDEATH`/`DDEATH` for `P01`; the bridge emits `AlarmSignal(LOW, DEVICE_OFFLINE)`.*

| Step | Observation | Result |
|------|-------------|--------|
| `emitDbirthAll()`, run 5 ticks                              | bridge alive cache populated | PASS |
| `emitDdeath(P01)`                                           | `drainEvents()` contains a `DEVICE_OFFLINE` alarm | PASS |

---

### T-6 `reconnect_emitsBridgeReconnectedAdvisory`

**Acceptance bullet:** *Bridge reconnect emits a `BRIDGE_RECONNECTED` advisory on the next reconnect.*

| Step | Observation | Result |
|------|-------------|--------|
| Birth all + 2 ticks                              | event queue empty                                       | PASS |
| `svc.onReconnected()`                            | `drainEvents()` contains `BRIDGE_RECONNECTED` alarm     | PASS |

---

### T-7 `audit_everyLineHasFrameworkShape`

**Acceptance bullet:** *The audit JSONL conforms to `00-FRAMEWORK §4`; the optional MQTT mirror carries the same records.*

| Step | Observation | Result |
|------|-------------|--------|
| 1500 ticks with the ramp scenario                    | audit file non-empty                              | PASS |
| Every JSONL line                                      | contains `ts, run, bridge, verdict, tag, safetyMode` | PASS |
| `bridge` field                                        | always `"mqtt"`                                   | PASS |

**Sample audit line** (APPLIED maintenance-window proposal):

```json
{"ts":1740001457000,"run":1780085513867,"bridge":"mqtt","verdict":"APPLIED","loopId":"PUMP-MAINT-ADV-P01","tag":"PLANT.PUMP.P01.MAINT_WINDOW","proposed":1892.235,"effective":1892.235,"safetyMode":"ADVISORY","evidenceNeurons":["140"]}
```

---

### T-8 `subscriptions_coverSparkplugGroup`

**Acceptance bullet:** *Sparkplug session model — the bridge subscribes to every birth/death/data topic under the configured group.*

| Step | Observation | Result |
|------|-------------|--------|
| `MqttClientService.start()` for the 20-pump config   | 6 group-level subscriptions registered            | PASS |
| Filters                                               | `NBIRTH+`, `NDEATH+`, `DBIRTH+/+`, `DDEATH+/+`, `NDATA+`, `DDATA+/+` | PASS |

---

### T-9 `Demo02ConfigYamlTest — documentedYaml_loadsAndMatchesInCodeConfig`

**Coverage:** Proves `demo02-pumps.yaml` parses under the loader's strict `FAIL_ON_UNKNOWN_PROPERTIES` contract and agrees exactly with `Demo02Config.build()`.

| Assertion | Expected | Result |
|-----------|----------|--------|
| Broker URL                  | `tcp://localhost:1883`                                  | PASS |
| Client ID                   | `jneopallium-pump-fleet`                                | PASS |
| Tick interval               | `PT1S`                                                  | PASS |
| Advisory queue size         | 10 000                                                  | PASS |
| Sparkplug enabled, group, edge | `true, Plant1, Jneopallium-Reliability`              | PASS |
| Read bindings count         | 2 (vibration_rms, bearing_temp for P01)                 | PASS |
| Write binding `PUMP-MAINT-ADV-P01` | advisoryTopic, signalTag, clamp `[0, 8760]`, qos 1 | PASS |
| `severityMap.HIGH_VIB`      | `HIGH`                                                  | PASS |
| `perTagSafetyMode.PUMP-MAINT-ADV-P01` | `ADVISORY`                                    | PASS |
| Audit local file path       | `/tmp/jneopallium-demo02-audit.jsonl`                   | PASS |
| YAML vs in-code cross-check | broker, sparkplug, topics, tags match `Demo02Config.build()` | PASS |

---

### T-10 `Demo02ConfigYamlTest — autonomousPromotion_isRejectedByLoader`

**Coverage:** Bridge-spec §3 / §9 S9 — `AUTONOMOUS` per-tag promotion is rejected at YAML load time, not just at in-code construction.

| Assertion | Result |
|-----------|--------|
| Loader throws on `perTagSafetyMode: { PUMP-MAINT-ADV-P01: AUTONOMOUS }` | PASS |
| Root cause message references the ADVISORY ceiling                     | PASS |

---

### T-11 `Demo02ConfigYamlTest — buildFleet_preservesAdvisoryCeiling`

**Coverage:** Every binding produced by `Demo02Config.build(20 pumps)` defaults to ADVISORY — the factory cannot leak an AUTONOMOUS write.

| Assertion | Result |
|-----------|--------|
| All 20 write bindings have `perTagSafetyMode == ADVISORY` | PASS |
| Read-binding count == 40 (20 pumps × 2 metrics)           | PASS |
| Write-binding count == 20 (one per pump)                  | PASS |

---

## Audit JSONL — Sample Lines

All lines produced by `MqttAuditOutput` conform to `00-FRAMEWORK §4`.

**APPLIED — maintenance window proposal published to the advisory namespace:**

```json
{"ts":1740001457000,"run":1780085513867,"bridge":"mqtt","verdict":"APPLIED","loopId":"PUMP-MAINT-ADV-P01","tag":"PLANT.PUMP.P01.MAINT_WINDOW","proposed":1892.235,"effective":1892.235,"safetyMode":"ADVISORY","evidenceNeurons":["140"]}
```

`proposed == effective` because the value (1892 h ahead) is well inside the `[0, 8760]` clamp; `loopId` matches the per-pump write binding; `safetyMode == ADVISORY` is the structural ceiling.

---

## Architecture Under Test

```
Pump fleet (P01 .. P20) — SimulatedPumpFleetMqttTransport
   spBv1.0/Plant1/DBIRTH/Edge-Pump/Pxx       {vibration_rms, bearing_temp}
   spBv1.0/Plant1/DDATA/Edge-Pump/Pxx
   spBv1.0/Plant1/DDEATH/Edge-Pump/Pxx
        │
        ▼
   MqttClientService (BIRTH cache, alive devices, Sparkplug session)
        │  Tahu decode  → MqttSignalMapper
        ▼
   MqttMetricInput(PUMP-VIB-Pxx)  ─┐
   MqttMetricInput(PUMP-TEMP-Pxx) │
   MqttEventInput(events)         │
                                  ▼
   ┌─────────────── PumpHealthSubnet ──────────────────────┐
   │ MeasurementValidatorNeuron  (range + rate of change)   │
   │ DegradationModelNeuron      (per-asset RUL, loop=2/3)  │
   │ MaintenanceSchedulingNeuron (window, loop=2/10)        │
   │ AlarmAggregationNeuron      (ISA-18.2 suppression)     │
   │ SafetyGateNeuron            (ADVISORY)                 │
   │ Edge-triggered proposal:                               │
   │   above-horizon → below-horizon ⇒ 1 SetpointSignal     │
   └────────────────────────┬───────────────────────────────┘
                            ▼ List<IResult>
   MqttAdvisoryOutputAggregator (publish-only, ADVISORY/SHADOW,
                                  [minClampValue, maxClampValue])
                            │
                            ▼
   advisory topic: spBv1.0/Plant1/DCMD/Edge-Pump/Pxx/advisory/maint_window
   + MqttAuditOutput JSONL (and optional MQTT mirror topic)
```

**Transport:** `SimulatedPumpFleetMqttTransport` (in-process, no broker) for tests and the runner. Swap to `DefaultMqttTransport` against a Mosquitto/EMQX/HiveMQ broker for over-the-wire runs; no other code changes.

---

## Configuration Highlights

| Knob | Value | Why |
|------|-------|-----|
| `wearPerVibUnit`        | 1.0 hours / (mm/s) / tick | matches `DegradationModelNeuron` default |
| `WEARING_VIB_RAMP`      | 0.005 mm/s per second     | P01 reaches ~8.5 mm/s by tick 1500 (under the validator's 20 mm/s ceiling) |
| `INITIAL_RUL_HOURS`     | 8 760 (1 year)            | spec-aligned baseline |
| `SCHEDULING_HORIZON`    | 2 000 hours               | triggers proposal when RUL falls below ~3 months |
| `MIN_LEAD_TIME_TICKS`   | 360 000 (≈100 hours)      | proposed window leads predicted EOL by ~100 hours |
| `TICKS_PER_HOUR`        | 3 600                     | matches `tickInterval: PT1S` |
| `ALARM_SUPPRESSION`     | 600 ticks                 | ISA-18.2 standing-alarm window |
| `MAX_ADVISORY_HOURS`    | 8 760                     | clamp ceiling — proposals can't propose 100 years out |

---

## Sparkplug topology

| Sparkplug address                                | Direction | Signal tag                | Loop / Binding         |
|--------------------------------------------------|-----------|---------------------------|------------------------|
| `Plant1/Edge-Pump/Pxx/vibration_rms`             | READ      | `PLANT.PUMP.Pxx.VIB`      | `PUMP-VIB-Pxx`         |
| `Plant1/Edge-Pump/Pxx/bearing_temp`              | READ      | `PLANT.PUMP.Pxx.BTEMP`    | `PUMP-TEMP-Pxx`        |
| `spBv1.0/Plant1/DCMD/Edge-Pump/Pxx/advisory/maint_window` | WRITE | `PLANT.PUMP.Pxx.MAINT_WINDOW` | `PUMP-MAINT-ADV-Pxx` |

The MD spec's wildcard form (`Plant1/Edge-Pump/+/vibration_rms`) is the doc-level shorthand for the per-pump bindings. The bridge's `MqttSignalMapper` derives the `MeasurementSignal#tag` from the single binding it matches, so per-asset RUL tracking requires one binding per pump in the actual loader-parseable config. `Demo02Config.build(pumpIds)` generates them; the YAML at `worker/src/test/resources/demo/demo02-pumps.yaml` demonstrates a 1-pump slice that the loader test pins.

---

## Safety / regulatory posture

The MQTT/Sparkplug bridge is **structurally ADVISORY**: any per-tag `AUTONOMOUS` promotion is rejected at config-load time (T-3, T-10). The bridge therefore cannot — by construction — actuate a field device through this protocol; it can only post maintenance-window suggestions to an advisory namespace that the operator HMI / CMMS picks up.

For actuating decisions, route through the OPC UA bridge ([demo 01](../../../../../../../../demo-01-reactor-cascade-control.md)) or PLC4X — both of which carry write-authority into the field. See `02-MQTT-SPARKPLUG.md §3 "Regulatory posture"` and `00-FRAMEWORK §7 "Per-loop deployment mode"`.
