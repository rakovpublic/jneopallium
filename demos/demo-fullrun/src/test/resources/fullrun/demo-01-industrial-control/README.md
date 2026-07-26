# Demo 01: Industrial Control

Story: an OPC-UA-like reactor control loop receives temperature, flow, valve feedback, alarm, and manual override signals from a deterministic local plant mock.

Network: sensor input, feature/error normalization, cascade control, safety gate, and result conversion layers.

Safety ceiling: `AUTONOMOUS-MOCK`. Commands are simulation-only. A forced high-temperature alarm triggers a fail-safe command within one tick, and manual override records a held command.
