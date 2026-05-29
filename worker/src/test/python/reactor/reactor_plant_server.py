#!/usr/bin/env python3
# Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
"""
asyncua OPC UA plant for demo-01 (reactor jacket-temperature cascade control).

Exposes the four nodes the bridge config in demo-01-reactor-cascade-control.md
binds, and integrates the same first-order-plus-dead-time CSTR model as the
in-process ``ReactorPlantSimulator`` so the Java demo behaves identically
whether it talks to this server or to the simulated transport:

    ns=2;s=Reactor.TIC101.PV        reactor temperature  [degC]   (read)
    ns=2;s=Reactor.FIC101.PV        coolant flow         [%]      (read)
    ns=2;s=Reactor.FIC101.OUT       jacket valve command [%]      (written by the bridge)
    ns=2;s=Reactor.HiTempInterlock  high-temperature interlock    (read)

Run:
    pip install -r requirements.txt
    python reactor_plant_server.py
Then point the bridge config endpointUrl at
    opc.tcp://localhost:4840/jneopallium/reactor

Force the interlock from another shell (or just raise the heat load) to watch
the bridge fail-safe the valve to 100 %.
"""
import asyncio

from asyncua import Server, ua

ENDPOINT = "opc.tcp://0.0.0.0:4840/jneopallium/reactor"
URI = "urn:rakovpublic:jneopallium:demo01"

# Plant parameters — identical to ReactorPlantSimulator.java
COOL_INLET = 20.0
THERMAL_MASS = 200.0
COOL_GAIN = 6.0
FLOW_TAU = 0.12
HI_TEMP_LIMIT = 130.0
DT = 0.1


class Plant:
    def __init__(self):
        self.heat_load = 120.0
        self.temp = 80.0
        self.flow = 50.0
        self.valve = 50.0

    def step(self, valve):
        self.valve = max(0.0, min(100.0, valve))
        a = min(1.0, DT / FLOW_TAU)
        self.flow += (self.valve - self.flow) * a
        self.flow = max(0.0, min(100.0, self.flow))
        removed = COOL_GAIN * (self.flow / 100.0) * (self.temp - COOL_INLET)
        self.temp += (self.heat_load - removed) / THERMAL_MASS * DT
        return self.temp >= HI_TEMP_LIMIT


async def main():
    server = Server()
    await server.init()
    server.set_endpoint(ENDPOINT)
    server.set_server_name("Jneopallium reactor plant")
    idx = await server.register_namespace(URI)  # first custom namespace -> ns=2

    objects = server.nodes.objects
    tic_pv = await objects.add_variable(ua.NodeId("Reactor.TIC101.PV", idx), "TIC101.PV", 80.0)
    fic_pv = await objects.add_variable(ua.NodeId("Reactor.FIC101.PV", idx), "FIC101.PV", 50.0)
    fic_out = await objects.add_variable(ua.NodeId("Reactor.FIC101.OUT", idx), "FIC101.OUT", 50.0)
    hi_ilk = await objects.add_variable(ua.NodeId("Reactor.HiTempInterlock", idx), "HiTempInterlock", False)
    await fic_out.set_writable()  # the bridge writes the valve command here

    plant = Plant()
    print(f"reactor plant up at {ENDPOINT} (ns={idx})")
    async with server:
        while True:
            valve = await fic_out.read_value()
            tripped = plant.step(float(valve))
            await tic_pv.write_value(round(plant.temp, 4))
            await fic_pv.write_value(round(plant.flow, 4))
            await hi_ilk.write_value(bool(tripped))
            await asyncio.sleep(DT)


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\nreactor plant stopped")
