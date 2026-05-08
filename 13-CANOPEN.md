# Bridge 13 — CANopen / CAN bus (embedded machines, vehicles, drives)

> **Prerequisite:** `00-FRAMEWORK.md`.

**Priority:** medium. **Safety ceiling:** `ADVISORY`. CANopen lives close to motors, brakes, and BMS — strict by default.

## 1. Domain context

[CANopen](https://www.can-cia.org/canopen/) is the application-layer protocol on top of the CAN bus, standardised by CiA (CAN in Automation). It's the dominant open standard for embedded networks in mobile robots, AGVs, industrial drives, medical machines, and automotive aftermarket gear. The spec defines:

* An **object dictionary** indexed by 16-bit object index + 8-bit subindex (`0x6041:00` is `statusword` for a CiA-402 servo drive).
* **Communication objects** — PDOs (cyclic, low-latency), SDOs (acyclic, request/reply), NMT (network state), heartbeat, sync.
* **Device profiles** — CiA-402 for motion control, CiA-401 for I/O, CiA-301 generic, etc.

For Jneopallium this is the embedded counterpart of the OPC UA / PLC4X bridges. Where OPC UA addresses *plant tags* and PLC4X addresses *PLC memory*, CANopen addresses *device-internal state*. The same `industrial/MeasurementSignal` and `industrial/ActuatorCommandSignal` types apply, plus `embodiment/ProprioceptiveSignal` for joint state and `industrial/EfficiencySignal` for BMS data.

## 2. Maven dependency

CAN access on Java is platform-specific. The pragmatic options:

```xml
<!-- SocketCAN (Linux) — most common on embedded Linux platforms.
     Requires `vcan0` or a real CAN interface. -->
<dependency>
    <groupId>com.willhaben</groupId>
    <artifactId>kafka-jcanlib</artifactId>          <!-- placeholder; verify -->
    <version>...</version>
</dependency>
```

In practice the dependable route is **JNA bindings to SocketCAN** plus a small CANopen-aware layer the bridge ships. Two routes the bridge implementation should evaluate:

* **`jSerialComm` + Lawicel-style USB-CAN adapter** — works on macOS / Windows / Linux; cheap dev hardware (CANable, Korlan, PCAN).
* **JNA + native SocketCAN** — Linux only, lowest latency, free.

The bridge spec does not prescribe a single library. The `CanopenClientService` is interface-first; the platform module is selected at startup.

## 3. Why advisory ceiling

CANopen is one to two metres from a moving motor. The aggregator therefore:

* Writes only to **non-actuating** indices by default (status setpoints, mode bytes for state-machine progression that an external supervisor confirms, parameters under operator-controlled gating).
* Cyclic motion-trajectory writes require an explicit `motionAdvisory: true` per binding plus the framework's `simulatorOnly` flag from the MAVLink playbook adapted for embedded contexts.
* PDO writes always go through the §2.2 algorithm; SDO writes are slow enough to log every one.

A CANopen bridge that writes a CiA-402 trajectory to a real drive without an external safety supervisor is misuse.

## 4. Architecture

```
embedded device fleet                  ┌────────────────────────┐
┌──────┐ ┌──────┐ ┌──────┐  CAN frames │ CanopenClientService   │
│ Drive│ │ BMS  │ │ I/O  │ ───────────▶│  • SocketCAN/USB-CAN   │
│ 0x01 │ │ 0x02 │ │ 0x10 │             │  • OD-aware decoder    │
└──────┘ └──────┘ └──────┘             │  • PDO mapping cache   │
                                       │  • per-node heartbeat  │
                                       └─────┬───────────┬──────┘
                                             │           │
                                       ┌─────▼─┐   ┌─────▼──────┐
                                       │ CanMea│   │ CanState   │
                                       │ sureme│   │ Input      │
                                       │ ntInpu│   │ → Proprio- │
                                       │ → Mea │   │   ceptive  │
                                       │   sure│   │ → Alarm    │
                                       │   ment│   │            │
                                       └───────┘   └────────────┘
                                                   ▼
                              [Pipeline → CanopenAdvisoryOutputAggregator]
                                                   ▼
                              SDO/PDO write → only to whitelisted indices
```

## 5. Signal mapping

| CANopen source | Decoded as | Jneopallium signal |
|---|---|---|
| TPDO numeric mapped to OD index (e.g. CiA-402 `0x6064:00 position_actual`) | int / float per OD type | `ProprioceptiveSignal` |
| TPDO state byte (CiA-402 `statusword 0x6041`) | enum | `BatchStateSignal` (drive state) + `AlarmSignal` if fault bits set |
| BMS state-of-charge / pack voltage / current | scalar | `EfficiencySignal` |
| EMCY (emergency) frame | error code | `AlarmSignal` (severity from EMCY error code lookup table) |
| Heartbeat timeout for a node | bridge state event | `AlarmSignal(HIGH, "NODE_OFFLINE")` |
| Sensor I/O via CiA-401 PDO | scalar | `MeasurementSignal` |

Egress (advisory, whitelisted indices only):

| Jneopallium signal | CANopen write | Notes |
|---|---|---|
| `SetpointSignal` (operator-confirmed) | RPDO or SDO to a config-whitelisted index | Whitelist enforced at config load. |
| `ActuatorCommandSignal` (state-machine transition only) | SDO to `controlword 0x6040` with permitted transitions only | Cannot write `Enable Operation` autonomously; only `Shutdown`/`Switch on disabled` allowed by default. |
| `MaintenanceWindowSignal` | none | Audit-only — operator schedules manually. |

## 6. Configuration

```yaml
canBus:
  type: "SOCKETCAN"                    # or "USB_CAN"
  device: "vcan0"
  bitrate: 500000
  samplePoint: 0.875

nodes:
  - id: 0x01
    type: "CiA-402"
    profileEdsFile: "/etc/jneopallium/eds/maxon_epos4.eds"
  - id: 0x02
    type: "CiA-418"                    # battery management system profile
    profileEdsFile: "/etc/jneopallium/eds/bms_pack_a.eds"

reads:
  - bindingId: "DRIVE-1-POS"
    nodeId: 0x01
    odIndex: 0x6064
    subIndex: 0x00
    pdoSource: "TPDO1"
    targetSignal: "PROPRIOCEPTIVE"
    signalTag: "DRIVE.1.POS_ACTUAL"

  - bindingId: "BMS-SOC"
    nodeId: 0x02
    odIndex: 0x2001                    # vendor-specific SOC
    subIndex: 0x00
    pdoSource: "TPDO1"
    targetSignal: "EFFICIENCY"
    signalTag: "BMS.PACK_A.SOC"

events:
  - bindingId: "DRIVE-1-FAULT"
    nodeId: 0x01
    source: "EMCY"
    targetSignal: "ALARM"
    signalTagPrefix: "DRIVE.FAULT"

writes:
  # Whitelisted index: trajectory profile parameter, NOT controlword.
  - bindingId: "DRIVE-1-PROFILE-VEL"
    nodeId: 0x01
    odIndex: 0x6081
    subIndex: 0x00
    odType: "UINT32"
    via: "SDO"
    signalTag: "DRIVE.1.PROFILE_VEL"
    minClampValue: 0
    maxClampValue: 5000

writeIndexAllowList:
  0x01:
    - 0x6081                           # profile_velocity
    - 0x607F                           # max_profile_velocity
    # 0x6040 controlword is NOT in this list

audit:
  localAuditFile: "/var/log/jneopallium/canopen-audit.jsonl"

perTagSafetyMode:
  DRIVE-1-PROFILE-VEL: ADVISORY
```

The validator rejects any write binding whose `(nodeId, odIndex)` isn't in `writeIndexAllowList`.

## 7. Package layout

```
worker/src/main/java/com/rakovpublic/jneuropallium/worker/bridge/canopen/
├── CanopenBridgeConfig.java
├── CanopenBridgeConfigLoader.java
├── EdsParser.java                     (Electronic Data Sheet parser)
├── ObjectDictionaryEntry.java
├── CanopenNodeBinding.java
├── CanopenSignalMapper.java
├── CanopenClientService.java          (interface)
├── SocketCanClientService.java        (Linux JNA implementation)
├── UsbCanClientService.java           (cross-platform via Lawicel/jSerialComm)
├── CanopenMeasurementInput.java
├── CanopenStateInput.java
├── CanopenEventInput.java
└── CanopenAdvisoryOutputAggregator.java
```

## 8. Phase plan

| Phase | Goal |
|-------|------|
| 1 | Read-only against `vcan0` virtual bus + `cangen`/`cansend` traffic generators. EDS parsing. PDO decoding. EMCY → AlarmSignal. |
| 2 | Read against a real drive on a USB-CAN dongle (Maxon EPOS4 or Igus dryve D1 are common dev targets). Multi-node config. |
| 3 | Advisory writes through SDO with whitelist. Operator confirmation in the runbook. |
| 4 | **Not pursued.** Cyclic motion-trajectory writes are deferred to a separately-scoped bridge with its own safety case. |

## 9. Bridge-specific scenarios

| # | Scenario | Setup | Expected |
|---|----------|-------|----------|
| **S7** | vcan0 read | `sudo modprobe vcan; sudo ip link add dev vcan0 type vcan; sudo ip link set up vcan0` then a `cangen` traffic generator | Bridge emits `ProprioceptiveSignal` per configured PDO mapping |
| **S8** | EDS parse | Load a real Maxon EPOS4 EDS | All TPDO/RPDO mappings discovered; OD type info attached to bindings |
| **S9** | Heartbeat loss | Stop sending heartbeat for node 0x01 | `AlarmSignal(HIGH, NODE_OFFLINE)` emitted; subsequent reads marked `Quality.UNCERTAIN` |
| **S10** | EMCY decode | Inject an EMCY frame with error code `0x2310` (continuous overcurrent) | `AlarmSignal(CRITICAL)` with the standard CANopen error description |
| **S11** | Disallowed index rejected | Config write binding for `0x6040` (controlword) not on allow list | `CanopenBridgeConfigLoader.load()` throws |
| **S12** | SDO write to allowed index | Pipeline emits a profile-velocity update | SDO write succeeds (or fails with audit `verdict=FAILED reason=SDO_ABORT_<code>`) |

## 10. Risks

| # | Risk | Mitigation |
|---|------|------------|
| R1 | Bus flooding from misconfigured PDO sync | Per-node decoder watchdog; if frame rate exceeds a per-binding cap, decimate with audit. |
| R2 | EDS dialect drift between vendors | Use a permissive parser that treats unknown sections as advisory; log unparsed entries as warnings. |
| R3 | Bricking a device by writing to a wrong index | The allow-list is the structural defence; document the procedure for adding an index (must include vendor confirmation that the index is safe to write at runtime). |
| R4 | Native SocketCAN on macOS / Windows | USB-CAN dongle path is the cross-platform escape hatch; document supported dongles. |

## 11. References

* CiA — CAN in Automation — `https://www.can-cia.org/`.
* CiA-402 (motion-control profile) overview — `https://www.can-cia.org/can-knowledge/canopen/cia402/`.
* Linux SocketCAN docs — `https://www.kernel.org/doc/html/latest/networking/can.html`.
