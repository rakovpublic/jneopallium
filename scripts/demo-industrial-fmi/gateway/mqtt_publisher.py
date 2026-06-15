from __future__ import annotations

import json
import time
from typing import Any, Dict


TELEMETRY_TOPICS = {
    "vibrationRms": "jneopallium/demo/skid/P101/vibration",
    "bearingTemperature": "jneopallium/demo/skid/P101/bearing-temperature",
    "pumpPowerKw": "jneopallium/demo/skid/P101/power-kw",
    "ambientTemperature": "jneopallium/demo/skid/environment/ambient-temperature",
}


class SkidMqttClient:
    def __init__(self, broker_host: str = "127.0.0.1", broker_port: int = 1883, client_id: str = "thermal-skid-gateway") -> None:
        try:
            import paho.mqtt.client as mqtt
        except ImportError as exc:  # pragma: no cover - optional runtime dependency
            raise RuntimeError("paho-mqtt is required for the MQTT gateway") from exc
        self._mqtt = mqtt
        self.client = mqtt.Client(client_id=client_id, protocol=mqtt.MQTTv5)
        self.host = broker_host
        self.port = broker_port
        self.connected = False

    def start(self) -> None:
        self.client.connect(self.host, self.port, keepalive=30)
        self.client.loop_start()
        self.connected = True

    def stop(self) -> None:
        self.connected = False
        self.client.loop_stop()
        self.client.disconnect()

    def publish_json(self, topic: str, payload: Dict[str, Any], qos: int = 1) -> None:
        if not self.connected:
            return
        self.client.publish(topic, json.dumps(payload, separators=(",", ":")), qos=qos, retain=False)

    def publish_metric(self, topic: str, value: Any, simulation_time: float) -> None:
        self.publish_json(
            topic,
            {
                "value": value,
                "quality": "GOOD",
                "timestamp": int(time.time() * 1000),
                "simulationTime": round(simulation_time, 3),
            },
        )
