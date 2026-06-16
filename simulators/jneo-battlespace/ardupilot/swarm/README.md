# ArduPilot Swarm Launch Notes

The live backend generates a deterministic `vehicle-map.json` before spawning
vehicles. Each vehicle must receive:

- unique `MAV_SYSID`
- unique SITL instance number
- unique MAVLink UDP port
- unique Gazebo model name
- unique ROS namespace
- unique camera topic
- unique output directory

The deterministic map uses `systemId = index`, `instance = index - 1`, and
`mavlinkEndpoint = udp://127.0.0.1:14550 + (index - 1) * 10`.

