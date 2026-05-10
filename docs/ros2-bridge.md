# ROS 2 / DDS bridge

> Companion to [`04-ROS2-DDS.md`](../04-ROS2-DDS.md) (the spec) and
> [`00-FRAMEWORK.md`](../00-FRAMEWORK.md) (the universal contract). This file
> documents the ROS 2 bridge's domain context, YAML schema, manual demo
> procedure, and regulatory posture — i.e. the §10 Definition-of-Done items
> that don't belong in the spec itself.

## Domain context

[ROS 2](https://docs.ros.org/) is the dominant middleware for modern
robotics; its standard transport is
[DDS](https://www.dds-foundation.org/) (Data Distribution Service). The
bridge implements **Strategy B** from the spec: a thin
JSON-over-WebSocket adapter on top of
[`rosbridge_suite`](https://github.com/RobotWebTools/rosbridge_suite),
backed by the JDK's built-in `java.net.http.WebSocket`. **Strategy A**
(an embedded `rcljava` client) is left as a feature flag in
`RcljavaClientService` and is not built into the v1 worker jar.

The bridge is **advisory-only** by construction (04-ROS2-DDS.md §3): it
subscribes to perception, proprioception and battery topics, emits typed
`SensorySignal` / `ProprioceptiveSignal` / `EfficiencySignal` /
`PeerObservationSignal` instances, and publishes neuron-derived
`MotorCommandSignal` / `FormationSignal` / `HarmVetoSignal` decisions
to a configurable advisory namespace consumed by an external autonomy
supervisor — never directly to `/cmd_vel`, `/joint_trajectory`, or
`/joint_command` in production. The forbidden-topic rule is enforced
both at config load (in `Ros2BridgeConfig`) and at runtime (in
`Ros2ClientService.publish`); per-tag `AUTONOMOUS` promotion is rejected
unless `simulatorOnly: true` is set, which models the
simulator-with-watchdog escape from §3.

## Components

```
worker/src/main/java/com/rakovpublic/jneuropallium/worker/bridge/ros2/
├── Ros2BridgeConfig.java               ← YAML record (immutable, /cmd_vel rejected)
├── Ros2BridgeConfigLoader.java         ← Jackson YAML loader, FAIL_ON_UNKNOWN
├── Ros2TopicBinding.java               ← read/write binding (BridgeBinding)
├── Ros2MessageMapper.java              ← ROS 2 JSON ↔ typed signal (pure)
├── Ros2Transport.java                  ← test-seam interface
├── RosbridgeTransport.java             ← Strategy B (rosbridge over JDK WebSocket)
├── RcljavaClientService.java           ← Strategy A placeholder (feature flag)
├── Ros2ClientService.java              ← lifecycle, latest cache, decimation, payload caps
├── Ros2SensoryInput.java               ← IInitInput → SensorySignal/PeerObservationSignal/AlarmSignal
├── Ros2StateInput.java                 ← IInitInput → ProprioceptiveSignal/EfficiencySignal
├── Ros2AuditOutput.java                ← local JSONL audit + optional ROS 2 mirror topic
├── Ros2AdvisoryOutputAggregator.java   ← IOutputAggregator (advisory publish only, with clamps)
└── package-info.java
```

## YAML schema

Refer to 04-ROS2-DDS.md §7 for the canonical example. Key points:

* `mode: ROSBRIDGE` (default) selects Strategy B; `RCLJAVA` selects
  Strategy A and currently fails fast since the dependency is not built
  in.
* `rosbridgeUrl` is the rosbridge_websocket URL (e.g.
  `ws://localhost:9090`). Required for `ROSBRIDGE` mode.
* `simulatorOnly: false` forbids `writes` whose `topic` is `/cmd_vel`,
  `/joint_trajectory`, or `/joint_command`. Setting it to `true`
  unlocks them — only do that when (a) the topics are namespaced under a
  simulator (Gazebo, Ignition, Webots), and (b) an external watchdog
  node will kill the bridge if heartbeats stop.
* Each `read` binding maps one ROS 2 topic to one signal class. The
  signal class is inferred from `msgType` if `signalKind` is not set:
  - `nav_msgs/msg/Odometry` → `ProprioceptiveSignal` (or
    `PeerObservationSignal` when `asPeerObservation: true`)
  - `sensor_msgs/msg/JointState` → `ProprioceptiveSignal`
  - `sensor_msgs/msg/LaserScan` → `SensorySignal` (`maxRangeBins`
    downsamples)
  - `sensor_msgs/msg/Image` / `CompressedImage` → `SensorySignal`
    (the bus carries a hash, never bytes — §5)
  - `sensor_msgs/msg/BatteryState` → `EfficiencySignal`
* `decimateBy` drops all but every Nth message at the binding (S10).
* `maxPayloadBytes` drops oversized messages with one audit entry per
  minute per binding (§10 R3).
* Each `write` binding clamps to `[minClampValue, maxClampValue]` and
  rate-limits to `rampRateMaxPerSec`. The aggregator currently clamps
  the dominant linear-x axis on a `Twist`; for other shapes it clamps
  the L2 magnitude.

## Manual demo (turtlesim)

Phase 1 acceptance test for the bridge follows §10 S7. Outside of
unit/integration tests, the manual demo is:

```bash
# 1. Bring up rosbridge_server and turtlesim in a ROS 2 (humble/iron/jazzy)
#    environment.
ros2 run rosbridge_server rosbridge_websocket
ros2 run turtlesim turtlesim_node

# 2. Point the bridge at the WS endpoint and bind /turtle1/pose.
cat <<EOF > /tmp/ros2-bridge.yaml
mode: ROSBRIDGE
rosbridgeUrl: "ws://localhost:9090"
simulatorOnly: true
reads:
  - bindingId: TURTLE-POSE
    topic: /turtle1/pose
    msgType: turtlesim/msg/Pose
    signalTag: TURTLE.T1.POSE
audit:
  localAuditFile: /tmp/jneopallium/ros2-audit.jsonl
EOF

# 3. Run a small Java program that loads the config, builds
#    Ros2ClientService + Ros2SensoryInput/Ros2StateInput, and prints the
#    signals it drains each tick. After ~2s the cache should contain
#    ProprioceptiveSignal instances reflecting turtle1's pose.
```

## Regulatory posture

The bridge ceiling is `ADVISORY` (04-ROS2-DDS.md §3). A robot can hurt
people: the bridge does **not** publish to a `cmd_vel`, `joint_trajectory`,
or `joint_command` topic in any production environment. The intended
steady state is:

* Bridge subscribes to perception / proprioception / battery topics →
  produces typed signals.
* Jneopallium swarm/planning module reasons over them.
* Bridge publishes to `mission_advice` / `formation_advisory` /
  `safety_veto` topics that an external **autonomy supervisor**
  consumes. The supervisor decides whether to hand the advice to
  actuators.

Direct `cmd_vel` publication is supported only against
`simulatorOnly: true`, in which case the operator runbook MUST
document the watchdog node that kills the bridge on a heartbeat
failure. Document `ROS_DOMAIN_ID` and `ROS_LOCALHOST_ONLY=1` in the
operator runbook to prevent DDS multicast leaking topics across
machines (§11 R2).
