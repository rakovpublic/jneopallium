from __future__ import annotations

import math
from dataclasses import dataclass


@dataclass(frozen=True)
class RadioNode:
    nodeId: str
    role: str
    x: float
    y: float
    rangeMeters: float = 260.0


@dataclass(frozen=True)
class RadioLink:
    source: str
    destination: str
    distanceMeters: float
    rssiDbm: float
    latencyMs: int
    packetLoss: float
    lineOfSight: bool


class RadioModel:
    def __init__(
        self,
        nodes: list[RadioNode],
        partitions: set[frozenset[str]] | None = None,
        disabled_nodes: set[str] | None = None,
    ) -> None:
        self.nodes = {node.nodeId: node for node in nodes}
        self.partitions = partitions or set()
        self.disabled_nodes = disabled_nodes or set()

    def link(self, source: str, destination: str) -> RadioLink | None:
        if source in self.disabled_nodes or destination in self.disabled_nodes:
            return None
        left = self.nodes[source]
        right = self.nodes[destination]
        if frozenset({source, destination}) in self.partitions:
            return None
        distance = math.hypot(left.x - right.x, left.y - right.y)
        max_range = min(left.rangeMeters, right.rangeMeters)
        if distance > max_range:
            return None
        line_of_sight = distance < max_range * 0.75
        loss = 0.02 if line_of_sight else 0.22
        rssi = -42.0 - distance / 7.5 - (12.0 if not line_of_sight else 0.0)
        latency = int(15 + distance / 8.0 + loss * 100)
        return RadioLink(
            source=source,
            destination=destination,
            distanceMeters=round(distance, 3),
            rssiDbm=round(rssi, 3),
            latencyMs=latency,
            packetLoss=round(loss, 3),
            lineOfSight=line_of_sight,
        )

    def neighbors(self, node_id: str) -> list[RadioLink]:
        links: list[RadioLink] = []
        for other in self.nodes:
            if other == node_id:
                continue
            link = self.link(node_id, other)
            if link is not None:
                links.append(link)
        return links

