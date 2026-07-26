# Demo 02: Pump Fleet Maintenance

Story: an MQTT/Sparkplug-like pump fleet stream monitors ten pumps with vibration, bearing temperature, and online/offline events.

Network: pump telemetry input, feature extraction, health estimation, advisory planning, and result conversion layers.

Safety ceiling: `ADVISORY`. The degrading pump receives maintenance advice, an offline pump receives an alarm advisory, and healthy pumps do not receive maintenance advice in the same window.
