from __future__ import annotations

import argparse
import asyncio
import json
import logging
from pathlib import Path
import time
from typing import Any, Dict

from fmu_adapter import FmpyFmuAdapter, PythonModelAdapter
from mqtt_publisher import SkidMqttClient, TELEMETRY_TOPICS
from opcua_server import SkidOpcUaServer
from scenario_loader import Scenario, load_scenario


OUTPUT_NAMES = [
    "processTemperature",
    "measuredTemperature",
    "circulationFlow",
    "suctionPressure",
    "coolingValvePosition",
    "pumpSpeedActual",
    "pumpPowerKw",
    "vibrationRms",
    "bearingTemperature",
    "highTemperatureInterlock",
    "lowFlowInterlock",
    "lowSuctionInterlock",
]


class PlantGateway:
    def __init__(
        self,
        scenario: Scenario,
        output_dir: Path,
        fmu_path: Path | None,
        mode: str,
        opcua_endpoint: str,
        mqtt_host: str,
        mqtt_port: int,
        step_size: float = 0.1,
        command_timeout: float = 2.0,
    ) -> None:
        self.scenario = scenario
        self.output_dir = output_dir
        self.mode = mode
        self.step_size = step_size
        self.command_timeout = command_timeout
        self.adapter = FmpyFmuAdapter(fmu_path) if fmu_path else PythonModelAdapter()
        self.opcua = SkidOpcUaServer(opcua_endpoint)
        self.mqtt = SkidMqttClient(mqtt_host, mqtt_port)
        self.command_last_value: Dict[str, float] = {
            "Skid.CV101.PositionCMD": 35.0,
            "Skid.P101.SpeedSP": 45.0,
            "Skid.HTR101.PowerCMD": 35.0,
        }
        self.command_last_seen: Dict[str, float] = {
            "Skid.CV101.PositionCMD": 0.0,
            "Skid.P101.SpeedSP": 0.0,
            "Skid.HTR101.PowerCMD": 0.0,
        }
        self.ambient_temperature = 23.0
        self.mqtt_available = True
        self.opcua_available = True
        self.log = logging.getLogger("plant_gateway")
        self.gateway_log = output_dir / "gateway.log"

    async def run(self) -> None:
        self.output_dir.mkdir(parents=True, exist_ok=True)
        logging.basicConfig(
            level=logging.INFO,
            format="%(asctime)s %(levelname)s %(message)s",
            handlers=[logging.FileHandler(self.gateway_log, encoding="utf-8"), logging.StreamHandler()],
        )
        self.adapter.initialize()
        for key, value in self.scenario.initial.items():
            self.adapter.set(key, value)
            if key == "ambientTemperature":
                self.ambient_temperature = float(value)
        await self.opcua.start()
        self.mqtt.start()
        await self.opcua.nodes.write("Skid.Simulation.Status", "RUNNING")
        self.log.info("gateway started scenario=%s mode=%s", self.scenario.name, self.mode)

        trace_path = self.output_dir / "gateway_trace.jsonl"
        events = sorted(self.scenario.events, key=lambda event: event.at)
        next_event = 0
        sim_time = 0.0
        last_publish = -1.0

        try:
            with trace_path.open("w", encoding="utf-8") as trace:
                start_wall = time.monotonic()
                while sim_time < self.scenario.durationSeconds:
                    while next_event < len(events) and events[next_event].at <= sim_time + 1e-9:
                        await self.apply_event(events[next_event])
                        next_event += 1
                    await self.apply_commands(sim_time)
                    self.adapter.do_step(sim_time, self.step_size)
                    sim_time += self.step_size
                    outputs = self.adapter.snapshot(OUTPUT_NAMES)
                    await self.write_outputs(outputs, sim_time)
                    if sim_time - last_publish >= 1.0 - 1e-9:
                        self.publish_telemetry(outputs, sim_time)
                        last_publish = sim_time
                    record = dict(outputs)
                    record["simulationTime"] = round(sim_time, 3)
                    trace.write(json.dumps(record, separators=(",", ":")) + "\n")
                    if self.mode == "REAL_TIME":
                        target = start_wall + sim_time
                        delay = target - time.monotonic()
                        if delay > 0:
                            await asyncio.sleep(delay)
        finally:
            await self.opcua.nodes.write("Skid.Simulation.Status", "STOPPING")
            self.mqtt.stop()
            await self.opcua.stop()
            self.adapter.close()
            self.log.info("gateway stopped scenario=%s", self.scenario.name)

    async def apply_event(self, event) -> None:
        for key, value in event.set.items():
            self.adapter.set(key, value)
            if key == "ambientTemperature":
                self.ambient_temperature = float(value)
        for key, value in event.opcua.items():
            await self.opcua.nodes.write(key, value)
        if event.mqtt_available is not None:
            self.mqtt_available = bool(event.mqtt_available)
        if event.opcua_available is not None:
            self.opcua_available = bool(event.opcua_available)
        self.log.info("scenario event at %.3f set=%s opcua=%s", event.at, event.set, event.opcua)

    async def apply_commands(self, sim_time: float) -> None:
        if not self.opcua_available:
            self.adapter.set("coolingValveCmd", 100.0)
            self.adapter.set("pumpSpeedCmd", 30.0)
            self.adapter.set("heaterPowerCmd", 0.0)
            return
        mapping = {
            "Skid.CV101.PositionCMD": ("coolingValveCmd", 100.0),
            "Skid.P101.SpeedSP": ("pumpSpeedCmd", 30.0),
            "Skid.HTR101.PowerCMD": ("heaterPowerCmd", 0.0),
        }
        for node, (variable, fail_safe) in mapping.items():
            value = float(await self.opcua.nodes.read(node))
            if abs(value - self.command_last_value[node]) > 1e-9:
                self.command_last_seen[node] = sim_time
                self.command_last_value[node] = value
            age = sim_time - self.command_last_seen.get(node, -1e9)
            self.adapter.set(variable, fail_safe if age > self.command_timeout else value)

    async def write_outputs(self, outputs: Dict[str, Any], sim_time: float) -> None:
        pairs = {
            "Skid.TIC101.PV": outputs["measuredTemperature"],
            "Skid.FIC101.PV": outputs["circulationFlow"],
            "Skid.P101.SpeedPV": outputs["pumpSpeedActual"],
            "Skid.CV101.PositionPV": outputs["coolingValvePosition"],
            "Skid.HTR101.PowerPV": self.command_last_value["Skid.HTR101.PowerCMD"],
            "Skid.SuctionPressure": outputs["suctionPressure"],
            "Skid.Interlock.HighTemperature": outputs["highTemperatureInterlock"],
            "Skid.Interlock.LowFlow": outputs["lowFlowInterlock"],
            "Skid.Interlock.LowSuctionPressure": outputs["lowSuctionInterlock"],
            "Skid.Simulation.Time": sim_time,
            "Skid.Simulation.Status": "RUNNING",
        }
        for key, value in pairs.items():
            await self.opcua.nodes.write(key, value)

    def publish_telemetry(self, outputs: Dict[str, Any], sim_time: float) -> None:
        if not self.mqtt_available:
            return
        for key, topic in TELEMETRY_TOPICS.items():
            value = self.ambient_temperature if key == "ambientTemperature" else outputs[key]
            self.mqtt.publish_metric(topic, value, sim_time)
        self.mqtt.publish_json(
            "jneopallium/demo/skid/status",
            {
                "value": "RUNNING",
                "quality": "GOOD",
                "timestamp": int(time.time() * 1000),
                "simulationTime": round(sim_time, 3),
            },
        )


def main() -> int:
    parser = argparse.ArgumentParser(description="Run the FMI plant gateway")
    parser.add_argument("--scenario", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--fmu", type=Path)
    parser.add_argument("--mode", choices=["REAL_TIME", "AS_FAST_AS_POSSIBLE"], default="REAL_TIME")
    parser.add_argument("--opcua-endpoint", default="opc.tcp://0.0.0.0:4840/jneopallium/skid/")
    parser.add_argument("--mqtt-host", default="127.0.0.1")
    parser.add_argument("--mqtt-port", type=int, default=1883)
    args = parser.parse_args()
    gateway = PlantGateway(
        scenario=load_scenario(args.scenario),
        output_dir=args.output_dir,
        fmu_path=args.fmu,
        mode=args.mode,
        opcua_endpoint=args.opcua_endpoint,
        mqtt_host=args.mqtt_host,
        mqtt_port=args.mqtt_port,
    )
    asyncio.run(gateway.run())
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
