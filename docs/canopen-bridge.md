# CANopen bridge

> Companion to [`13-CANOPEN.md`](../13-CANOPEN.md) (the spec) and
> [`00-FRAMEWORK.md`](../00-FRAMEWORK.md) (the universal contract). This
> file documents the CANopen bridge's domain context, YAML schema, manual
> demo procedure, and regulatory posture — i.e. the §10
> Definition-of-Done items that don't belong in the spec itself.

## Domain context

[CANopen](https://www.can-cia.org/canopen/) is the application-layer
protocol on top of the CAN bus (CiA, CAN in Automation). It addresses
device-internal state via an Object Dictionary indexed by 16-bit object
index plus 8-bit subindex (e.g. `0x6041:00 statusword`,
`0x6064:00 position_actual`). The dominant device profiles are CiA-301
(generic), CiA-401 (I/O), CiA-402 (motion control), and CiA-418 (battery
management).

The bridge surfaces TPDO scalar payloads, EMCY emergency frames, and
heartbeat liveness onto the Jneopallium signal bus and writes neuron-
derived setpoints back through SDO or RPDO — but *only* to non-actuating
indices that an operator has explicitly placed on the per-node
`writeIndexAllowList`. The CiA-402 `controlword` (`0x6040`) is on a
hard-coded **forbidden** list and is rejected at config load even when
re-allowed in the user-facing list.

## Components

```
worker/src/main/java/com/rakovpublic/jneuropallium/worker/bridge/canopen/
├── CanopenBridgeConfig.java                ← YAML record (immutable, 0x6040 rejected)
├── CanopenBridgeConfigLoader.java          ← Jackson YAML loader, FAIL_ON_UNKNOWN
├── EdsParser.java                          ← Permissive Electronic Data Sheet parser (CiA-306)
├── ObjectDictionaryEntry.java              ← One OD entry: index, subindex, type, access
├── CanopenNodeBinding.java                 ← read/event/write binding (BridgeBinding)
├── CanopenSignalMapper.java                ← PDO/EMCY/heartbeat ↔ typed signal (pure)
├── CanFrame.java                           ← raw CAN frame record
├── CanopenClientService.java               ← interface (orchestrator surface)
├── AbstractCanopenClientService.java       ← shared orchestration: routing, watchdog, audit, queueing
├── SocketCanClientService.java             ← Linux SocketCAN backend (sink-injected)
├── UsbCanClientService.java                ← Cross-platform Lawicel USB-CAN backend (sink-injected)
├── CanopenMeasurementInput.java            ← IInitInput → MeasurementSignal/EfficiencySignal
├── CanopenStateInput.java                  ← IInitInput → ProprioceptiveSignal + statusword carrier
├── CanopenEventInput.java                  ← IInitInput → AlarmSignal (EMCY, heartbeat-loss)
├── CanopenAuditOutput.java                 ← local JSONL audit
├── CanopenAdvisoryOutputAggregator.java    ← IOutputAggregator (SDO + RPDO advisory egress)
└── package-info.java
```

## YAML schema

Refer to 13-CANOPEN.md §6 for the canonical example. Key points:

* `canBus.type` is `SOCKETCAN` (Linux production) or `USB_CAN`
  (cross-platform dev hardware: CANable / Korlan / PCAN-USB in Lawicel
  mode). `device` is an interface name (`vcan0`, `can0`) for SocketCAN
  or a serial port path (`/dev/ttyACM0`, `COM4`) for USB-CAN.
* `nodes` lists every CANopen node id (1..127) the bridge expects to
  see. `profileEdsFile` is optional — when present the bridge parses
  the EDS via `EdsParser` so the OD entry types are available for
  cross-checks against the per-binding `odType`.
* Each `read` binding maps one `(nodeId, odIndex, subIndex, pdoSource)`
  tuple to one signal class. The class is set by `targetSignal`:
  - `PROPRIOCEPTIVE` — joint state / encoder PDO from a CiA-402 drive
  - `MEASUREMENT` — generic CiA-401 I/O / sensor PDO; carries
    `Quality.UNCERTAIN` while the source node is past heartbeat timeout
  - `EFFICIENCY` — CiA-418 battery / pack metric (the mapper assumes
    0..100 % SoC inputs and divides by 100 to land in 0..1)
  - `BATCH_STATE` — CiA-402 statusword; the queue carries a
    `MeasurementSignal` (because `BatchStateSignal` is not an
    `IInputSignal`) plus an `AlarmSignal` if bit 3 (`FAULT`) is set; the
    typed `BatchStateSignal` is available via
    `AbstractCanopenClientService.lastDriveState(bindingId)`.
* `events` are EMCY (CiA-301 emergency frame, COB-ID `0x080 + nodeId`)
  or `HEARTBEAT_LOSS` (synthesised by the bridge's watchdog).
* Each `write` binding clamps to `[minClampValue, maxClampValue]` and
  rate-limits to `rampRateMaxPerSec`. The egress path is `SDO` (acyclic,
  audited per write) or `RPDO` (cyclic). **Every write binding must clear
  two structural gates**:
  1. The hard-coded forbidden-OD-index list rejects `controlword`
     (`0x6040`) unconditionally.
  2. The per-node `writeIndexAllowList` must include the index. A typo
     here is caught at load.
* `decimateBy` on a read binding drops all but every Nth message.

## Heartbeat liveness

`AbstractCanopenClientService.checkHeartbeats(nowMs)` should be called
once per tick by the host loop. For every node from which a heartbeat
frame (COB-ID `0x700 + nodeId`) has ever arrived, if more than 2 s
elapses without a fresh one, the bridge:

* Emits a `NODE_OFFLINE` `AlarmSignal` (priority `HIGH`) on the global
  event channel.
* Marks the node "offline" — subsequent `MeasurementSignal` reads from
  bindings on that node carry `Quality.UNCERTAIN` per 00-FRAMEWORK
  §0.5.
* If a `HEARTBEAT_LOSS` event-binding is configured for the node, the
  alarm also lands on that binding's queue (so a per-binding input
  drains it).

The alarm is sticky — it fires once per online → offline transition,
not on every tick — so the audit channel doesn't drown in repeats.

## Manual demo (vcan0)

Phase 1 acceptance test for the bridge follows §10 S7. Outside of
unit/integration tests, the manual demo is:

```bash
# 1. Bring up a virtual CAN interface on Linux.
sudo modprobe vcan
sudo ip link add dev vcan0 type vcan
sudo ip link set up vcan0

# 2. Point the bridge at vcan0.
cat <<EOF > /tmp/canopen-bridge.yaml
canBus:
  type: SOCKETCAN
  device: vcan0
  bitrate: 500000

nodes:
  - id: 1
    type: CiA-402
  - id: 2
    type: CiA-418

reads:
  - bindingId: DRIVE-1-POS
    nodeId: 1
    odIndex: 24676   # 0x6064 position_actual
    subIndex: 0
    pdoSource: TPDO1
    odType: INT32
    targetSignal: PROPRIOCEPTIVE
    signalTag: DRIVE.1.POS_ACTUAL
  - bindingId: BMS-SOC
    nodeId: 2
    odIndex: 8193   # 0x2001 vendor SOC
    subIndex: 0
    pdoSource: TPDO1
    odType: UINT16
    targetSignal: EFFICIENCY
    signalTag: BMS.PACK_A.SOC

events:
  - bindingId: DRIVE-1-FAULT
    nodeId: 1
    source: EMCY
    targetSignal: ALARM
    signalTagPrefix: DRIVE.FAULT

writes:
  - bindingId: DRIVE-1-PROFILE-VEL
    nodeId: 1
    odIndex: 24705   # 0x6081 profile_velocity
    subIndex: 0
    odType: UINT32
    via: SDO
    signalTag: DRIVE.1.PROFILE_VEL
    minClampValue: 0
    maxClampValue: 5000

writeIndexAllowList:
  1:
    - 24705   # 0x6081 profile_velocity (controlword 0x6040 deliberately omitted)

audit:
  localAuditFile: /var/log/jneopallium/canopen-audit.jsonl

perTagSafetyMode:
  DRIVE-1-PROFILE-VEL: ADVISORY
EOF

# 3. Generate traffic with the canutils — cangen for periodic random
#    frames, cansend for shaped frames:
cangen -g 50 -I 0x181 -L 4 vcan0   # TPDO1 from node 1, 4-byte payload
cansend vcan0 081#0023020000000000  # EMCY from node 1, code 0x2300
cansend vcan0 701#05                # NMT operational heartbeat from node 1

# 4. Run a small Java program that loads the YAML, builds
#    SocketCanClientService + CanopenStateInput / CanopenMeasurementInput
#    / CanopenEventInput, and prints the signals it drains each tick.
```

A real-hardware demo (§10 phase 2) follows the same recipe with
`device: can0` and a Maxon EPOS4 / Igus dryve D1 drive on a USB-CAN
dongle (`device: /dev/ttyACM0`, `type: USB_CAN`).

## Regulatory posture

The bridge ceiling is `ADVISORY` (13-CANOPEN.md §3, §11.13). CANopen
sits a metre or two from a moving motor, so:

* The **forbidden-OD-index list** is the structural, code-level defence.
  Adding to it requires editing
  `CanopenBridgeConfig.FORBIDDEN_WRITE_OD_INDICES` — a code change with a
  PR and a review, not a config tweak.
* The **per-node writeIndexAllowList** is the operator-facing gate.
  Adding an index there requires vendor confirmation that the index is
  safe to write at runtime; the procedure belongs in the deployment
  runbook.
* PDO writes go through the universal §2.2 algorithm; SDO writes are
  slow enough (millisecond-scale, bus-arbitration limited) to log every
  one.
* Cyclic motion-trajectory writes (the kind a CiA-402 servo controller
  expects 1 kHz) are explicitly out of scope (§8 phase 4) — they
  require a separately scoped bridge with its own safety case.

A CANopen bridge that writes a CiA-402 trajectory to a real drive
without an external safety supervisor is misuse.
