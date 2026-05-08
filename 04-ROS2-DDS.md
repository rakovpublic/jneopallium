# Bridge 04 — ROS 2 / DDS (robotics and swarm)

> **Prerequisite:** `00-FRAMEWORK.md`.

**Priority:** very high. **Safety ceiling:** `ADVISORY` initially. `AUTONOMOUS` is permitted only for *simulator* targets (Gazebo, Ignition, Webots) and only with an external watchdog.

## 1. Domain context

[ROS 2](https://docs.ros.org/) (Robot Operating System 2) is the dominant middleware for modern robotics. Its standard transport is [DDS](https://www.dds-foundation.org/) (Data Distribution Service); the ROS 2 client libraries (rclcpp, rclpy, rclnodejs, rcljava — variably maintained) talk to underlying DDS implementations (Cyclone DDS, Fast DDS, Connext).

There are two viable bridge strategies:

* **Strategy A — embedded ROS 2 client:** Use `rcljava` (community-maintained) inside the JVM. Lowest latency, single process. **Risk:** rcljava is community-led; lifecycle and feature parity with rclcpp lag.
* **Strategy B — `ros2_bridge` / `rosbridge_suite` over WebSocket:** Run a `rosbridge_server` ROS 2 node alongside Jneopallium. Use a Java WebSocket client to talk JSON-encoded ROS 2 messages. Higher latency but trivial to deploy.

For an MVP, Strategy B is correct. Production deployments revisit Strategy A or run the bridge as a sidecar.

## 2. Maven dependencies

### Strategy B (recommended initial)
```xml
<!-- WebSocket client (HiveMQ ships one already; reuse if MQTT bridge is in
     the same deployment) -->
<dependency>
    <groupId>org.java-websocket</groupId>
    <artifactId>Java-WebSocket</artifactId>
    <version>1.5.7</version>
</dependency>
```

Pair with a `rosbridge_suite` ROS 2 node at runtime:
```bash
ros2 run rosbridge_server rosbridge_websocket
```

### Strategy A (advanced)
```xml
<dependency>
    <groupId>org.ros2</groupId>
    <artifactId>rcljava</artifactId>
    <version>0.9.0</version>   <!-- verify; rcljava versions track ROS distros -->
</dependency>
```

Strategy A requires a ROS 2 distribution installed on the host (`humble`, `iron`, or `jazzy`).

## 3. Why advisory only initially

A robot can hurt people. The bridge does **not** publish to a `cmd_vel`, `joint_trajectory`, or `joint_command` topic in any production environment, with one exception: simulator targets clearly marked in YAML and gated by an external watchdog node that kills the bridge if heartbeats stop.

The intended steady state is:

* Bridge subscribes to perception, proprioception, and battery topics → produces typed signals.
* Jneopallium swarm/planning module reasons over them.
* Bridge publishes to `mission_advice` / `formation_advisory` / `safety_veto` topics that an external **autonomy supervisor** consumes. The supervisor decides whether to hand the advice to actuators.
* Direct `cmd_vel` publication is supported only against a `simulator-only: true` flag in the YAML, with a hard-coded refusal if the topic name doesn't match a configured simulator namespace.

## 4. Architecture

```
┌──────────────────────┐    /camera, /lidar, /odom,        ┌──────────────────────┐
│ ROS 2 graph:         │    /joint_states, /battery        │ Ros2ClientService    │
│  • robot nodes       │ ─────────────────────────────────▶│  • rosbridge ws OR   │
│  • simulator         │                                   │    rcljava nodes     │
│  • autonomy          │ ◀──── /mission_advice,            │  • per-topic latest  │
│    supervisor        │      /formation_advisory,         │    cache             │
└──────────────────────┘      /safety_veto                 └──────┬──────────────┬┘
                                                                  │              │
                                                       ┌──────────▼┐  ┌──────────▼┐
                                                       │ Ros2Sense │  │ Ros2State │
                                                       │ Input     │  │ Input     │
                                                       │ → Sensory │  │ → Proprio │
                                                       │   Signal  │  │   Energy  │
                                                       │   Peer    │  │           │
                                                       │   Obs     │  │           │
                                                       └───────────┘  └───────────┘
                                                                ▼
                              [Pipeline → Ros2AdvisoryOutputAggregator]
                                                                ▼
                                  publish to advisory namespace, NOT cmd_vel
```

## 5. Signal mapping

Reuses `ai/signals/fast/SensorySignal`, `embodiment/`, `swarm/`, `industrial/EfficiencySignal` (for energy).

| ROS 2 topic | Type | Jneopallium signal | Notes |
|---|---|---|---|
| `/camera/image_raw` | `sensor_msgs/Image` | `SensorySignal(modality=VISION)` | Carry image as a reference (URI/sha) not bytes; large payloads are bad on the bus. |
| `/scan` (lidar) | `sensor_msgs/LaserScan` | `SensorySignal(modality=LIDAR)` | Range array attached; downsample if too large per binding `maxRangeBins`. |
| `/odom` | `nav_msgs/Odometry` | `ProprioceptiveSignal` | Pose + velocity. |
| `/joint_states` | `sensor_msgs/JointState` | `ProprioceptiveSignal` (one per joint) | High-frequency; consider a per-binding decimation factor. |
| `/battery_state` | `sensor_msgs/BatteryState` | `EfficiencySignal` (or new `EnergyStateSignal` — see §6) | |
| `/peer_<id>/odom` | `nav_msgs/Odometry` | `PeerObservationSignal` | One bridge per swarm member, or a multiplexed bridge. |

Egress (advisory only):

| Jneopallium signal | ROS 2 publish | Notes |
|---|---|---|
| `MotorCommandSignal` | `<advisory_ns>/cmd_vel_advice` `geometry_msgs/Twist` | NEVER `/cmd_vel`. |
| `FormationSignal` | `<advisory_ns>/formation_advisory` (custom msg) | |
| `HarmVetoSignal` | `<advisory_ns>/safety_veto` `std_msgs/String` | Highest priority; supervisor must subscribe. |

## 6. New signal proposed (optional)

`embodiment/EnergyStateSignal` — battery percentage, voltage, current, charging flag. **Optional** because `EfficiencySignal` covers the analytical case; the new signal would carry richer state for direct robot energy reasoning. Defer to v2.

## 7. Configuration

```yaml
ros2:
  mode: "ROSBRIDGE"                      # or "RCLJAVA"
  rosbridgeUrl: "ws://localhost:9090"
  domainId: 0                            # only for RCLJAVA mode
  qosProfile: "SENSOR_DATA"              # or "RELIABLE", "PARAMETERS"

simulatorOnly: false                     # true unlocks /cmd_vel publish

reads:
  - bindingId: "ROBOT-ODOM"
    topic: "/robot1/odom"
    msgType: "nav_msgs/msg/Odometry"
    signalTag: "ROBOT.R1.ODOM"

  - bindingId: "PEER-ODOM-2"
    topic: "/robot2/odom"
    msgType: "nav_msgs/msg/Odometry"
    signalTag: "ROBOT.R2.PEER_ODOM"
    asPeerObservation: true              # promotes to PeerObservationSignal

writes:
  - bindingId: "MISSION-ADVICE"
    topic: "/jneopallium/advisory/mission_advice"
    msgType: "std_msgs/msg/String"
    signalTag: "ROBOT.R1.MISSION_ADV"

audit:
  localAuditFile: "/var/log/jneopallium/ros2-audit.jsonl"

perTagSafetyMode:
  MISSION-ADVICE: ADVISORY
```

The validator **rejects** any write binding whose topic matches `/cmd_vel`, `/joint_trajectory`, or `/joint_command` unless `simulatorOnly: true`.

## 8. Package layout

```
worker/src/main/java/com/rakovpublic/jneuropallium/worker/bridge/ros2/
├── Ros2BridgeConfig.java
├── Ros2BridgeConfigLoader.java
├── Ros2TopicBinding.java
├── Ros2MessageMapper.java
├── Ros2ClientService.java                (interface)
├── RosbridgeClientService.java           (Strategy B implementation)
├── RcljavaClientService.java             (Strategy A; behind a feature flag)
├── Ros2SensoryInput.java
├── Ros2StateInput.java
└── Ros2AdvisoryOutputAggregator.java
```

## 9. Phase plan

| Phase | Goal |
|-------|------|
| 1 | Strategy B (rosbridge), read-only, against `turtlesim` and Gazebo simulator. SensorySignal + ProprioceptiveSignal. |
| 2 | Advisory publish to a dedicated namespace. Validator rejects `/cmd_vel` outside simulator mode. |
| 3 | (Optional) Strategy A with rcljava for low-latency deployments. Keep behind a feature flag. |

## 10. Bridge-specific scenarios

| # | Scenario | Setup | Expected |
|---|----------|-------|----------|
| **S7** | turtlesim subscribe | `ros2 run turtlesim turtlesim_node`, bind `/turtle1/pose` | `MeasurementSignal` cache populated within 2 s |
| **S8** | cmd_vel rejected | Config has a write binding for `/cmd_vel`, `simulatorOnly: false` | `Ros2BridgeConfigLoader.load()` throws |
| **S9** | Multi-robot peer observations | Bind `/robot2/odom` with `asPeerObservation: true` | `PeerObservationSignal` (not `ProprioceptiveSignal`) is emitted |
| **S10** | Big payload throttling | Camera bound at 30 fps native, decimate=10 | Bridge emits sensory signals at 3 fps; logs no missed-message warnings |
| **S11** | Reconnect after rosbridge restart | Kill and restart `rosbridge_server` | Bridge reconnects, advisory `AlarmSignal(BRIDGE_RECONNECTED)` emitted |

## 11. Risks

| # | Risk | Mitigation |
|---|------|------------|
| R1 | rcljava maintenance gap | Default to Strategy B; Strategy A is opt-in. |
| R2 | DDS multicast misconfiguration leaking topics across machines | Document `ROS_DOMAIN_ID` and `ROS_LOCALHOST_ONLY=1` in the operator runbook. |
| R3 | Image/PointCloud message size DOS | Per-binding payload size caps; bridge drops oversize messages with a single audit entry per minute. |
| R4 | Time sync between bridge JVM and ROS 2 nodes | Use ROS 2 message header timestamps as the source of truth (framework §0.6); document NTP requirement. |

## 12. References

* ROS 2 docs — `https://docs.ros.org/en/humble/`.
* rosbridge_suite — `https://github.com/RobotWebTools/rosbridge_suite`.
* DDS Foundation — `https://www.dds-foundation.org/`.
