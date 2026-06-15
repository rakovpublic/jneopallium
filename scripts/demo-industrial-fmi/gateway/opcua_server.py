from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Dict


OPCUA_NODES: Dict[str, Any] = {
    "Skid.TIC101.PV": 45.0,
    "Skid.TIC101.SP": 70.0,
    "Skid.FIC101.PV": 0.55,
    "Skid.P101.SpeedPV": 45.0,
    "Skid.P101.SpeedSP": 45.0,
    "Skid.CV101.PositionPV": 35.0,
    "Skid.CV101.PositionCMD": 35.0,
    "Skid.HTR101.PowerPV": 35.0,
    "Skid.HTR101.PowerCMD": 35.0,
    "Skid.SuctionPressure": 0.8,
    "Skid.Interlock.HighTemperature": False,
    "Skid.Interlock.LowFlow": False,
    "Skid.Interlock.LowSuctionPressure": False,
    "Skid.Operator.ManualMode": False,
    "Skid.Operator.ManualValveCommand": 35.0,
    "Skid.Operator.ManualPumpCommand": 45.0,
    "Skid.Simulation.Time": 0.0,
    "Skid.Simulation.Status": "STARTING",
}


@dataclass
class OpcUaNodeSet:
    nodes: Dict[str, Any]

    async def read(self, name: str) -> Any:
        node = self.nodes[name]
        return await node.read_value()

    async def write(self, name: str, value: Any) -> None:
        await self.nodes[name].write_value(value)


class SkidOpcUaServer:
    def __init__(self, endpoint: str = "opc.tcp://0.0.0.0:4840/jneopallium/skid/") -> None:
        try:
            from asyncua import Server, ua
        except ImportError as exc:  # pragma: no cover - optional runtime dependency
            raise RuntimeError("asyncua is required for the OPC UA gateway") from exc
        self._server = Server()
        self._ua = ua
        self.endpoint = endpoint
        self.idx = 2
        self.nodes = OpcUaNodeSet({})

    async def start(self) -> None:
        await self._server.init()
        self._server.set_endpoint(self.endpoint)
        self.idx = await self._server.register_namespace("urn:jneopallium:demo:skid")
        objects = self._server.nodes.objects
        for name, initial in OPCUA_NODES.items():
            node = await objects.add_variable(self._ua.NodeId.from_string(f"ns={self.idx};s={name}"), name, initial)
            await node.set_writable()
            self.nodes.nodes[name] = node
        await self._server.start()

    async def stop(self) -> None:
        await self._server.stop()
