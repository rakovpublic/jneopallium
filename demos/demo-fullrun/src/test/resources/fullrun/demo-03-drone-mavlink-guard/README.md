# Demo 03: Drone MAVLink Guard

Story: a MAVLink-style mission stream sends battery, GPS, altitude, geofence distance, and mission command telemetry to a simulated mission guard.

Network: drone telemetry input, risk feature extraction, mission guard/veto, and result conversion layers.

Safety ceiling: `SIM-ONLY`. Normal commands are allowed, geofence-violating commands are vetoed, and low battery produces a return-to-home advisory.
