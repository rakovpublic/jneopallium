# Industrial FMI Demo Report

Generated with:

```bash
python scripts/demo-industrial-fmi/run_demo.py all
```

## Measured Metrics

Latest local run:

| Scenario | IAE | Overshoot | Settling s | Energy kWh | Travel | Reversals | Safety-out s | Fault delay s | Interlock latency s | MQTT outage availability |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| normal | 1661.70403 | 1.24352 | 136.6 | 0.5625329 | 84.2206 | 351 | 0.0 | n/a | n/a | n/a |
| load-disturbance | 1666.27019 | 2.6493 | n/a | 0.5480795 | 96.60229 | 361 | 0.0 | n/a | n/a | n/a |
| oscillation | 1566.67802 | 0.7932 | 125.0 | 0.5149163 | 82.80291 | 289 | 0.0 | n/a | n/a | n/a |
| pump-wear | 1632.55058 | 0.0 | 136.6 | 0.5306183 | 86.14329 | 88 | 0.0 | 58.4 | n/a | n/a |
| temperature-sensor-drift | 1422.495 | 2.36012 | n/a | 0.4402465 | 86.91421 | 260 | 0.0 | n/a | n/a | n/a |
| mqtt-outage | 1579.13528 | 0.0 | n/a | 0.4531485 | 40.15263 | 0 | 0.0 | n/a | n/a | 1.0 |
| opcua-outage | 1818.33862 | 0.0 | n/a | 0.4094464 | 287.51978 | 2 | 0.0 | n/a | n/a | n/a |
| high-temperature-interlock | 1669.2868 | 45.04625 | n/a | 0.0758735 | 297.91301 | 3 | 31.9 | n/a | 0.1 | n/a |
| operator-override | 1658.56883 | 0.0 | n/a | 0.4713636 | 138.15263 | 2 | 0.0 | n/a | n/a | n/a |

## Implemented Behavior

- FMI source is under `scripts/demo-industrial-fmi/plant/` and builds `ThermalSkid.fmu` with `pythonfmu`.
- The protocol gateway advances the plant at a fixed 0.1 second step and exposes OPC UA plus MQTT.
- The Java controller package is `com.rakovpublic.jneuropallium.worker.demo.industrialfmi`.
- OPC UA command aggregation reuses the existing `OpcUaCommandOutputAggregator`.
- MQTT advisory output reuses the existing `MqttAdvisoryOutputAggregator`.
- MQTT autonomous mode is rejected by configuration.

## Limitations

- The default one-command runner creates deterministic evidence without requiring Docker, a broker, or an OPC UA server in the test environment.
- The full protocol path is provided through `plant_gateway.py` plus `IndustrialFmiDemoMain`, but it requires Maven, Mosquitto/Docker, Python gateway dependencies, and a built FMU.
- The safety model is a simulation demonstration, not certified SIS/PLC logic.
