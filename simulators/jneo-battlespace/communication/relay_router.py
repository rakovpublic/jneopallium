from __future__ import annotations

from collections import deque
from dataclasses import asdict

from communication.radio_model import RadioModel


class RelayRouter:
    """Routes application payloads through the constrained radio model."""

    def __init__(self, radio: RadioModel, retranslators: set[str]) -> None:
        self.radio = radio
        self.retranslators = retranslators

    def route(self, source: str, destination: str, max_hops: int = 4) -> list[str] | None:
        queue: deque[list[str]] = deque([[source]])
        visited = {source}
        while queue:
            path = queue.popleft()
            if len(path) - 1 > max_hops:
                continue
            last = path[-1]
            if last == destination:
                return path
            for link in self.radio.neighbors(last):
                nxt = link.destination
                if nxt in visited:
                    continue
                if nxt != destination and nxt not in self.retranslators:
                    continue
                visited.add(nxt)
                queue.append(path + [nxt])
        return None

    def transmission_events(
        self,
        route: list[str] | None,
        message_id: str,
        simulation_time: float,
        payload_kind: str = "target-report",
    ) -> list[dict]:
        if route is None:
            return [
                {
                    "messageId": message_id,
                    "simulationTime": simulation_time,
                    "payloadKind": payload_kind,
                    "event": "DROP",
                    "reason": "NO_PERMITTED_RADIO_ROUTE",
                }
            ]
        events: list[dict] = []
        for index in range(len(route) - 1):
            source = route[index]
            destination = route[index + 1]
            link = self.radio.link(source, destination)
            base = {
                "messageId": message_id,
                "simulationTime": simulation_time,
                "payloadKind": payload_kind,
                "hop": index + 1,
                "source": source,
                "destination": destination,
                "link": asdict(link) if link is not None else None,
            }
            events.append(base | {"event": "SEND"})
            events.append(base | {"event": "RECEIVE"})
            if destination != route[-1]:
                events.append(base | {"event": "FORWARD", "forwarder": destination})
        return events

