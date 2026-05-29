# demo-01 reactor plant (OPC UA)

Implements [`demo-01-reactor-cascade-control.md`](../../../../../demo-01-reactor-cascade-control.md)
— the flagship closed-loop control demo.

There are two ways to run the demo; both drive the **same** Jneopallium code
(`OpcUaMeasurementInput` / `OpcUaAlarmInput` → industrial neuron pipeline →
`OpcUaCommandOutputAggregator` → `OpcUaTransparencyLogOutput`):

## 1. In-process (no network, deterministic) — default

`ReactorPlantSimulator` is wired to the bridge through
`SimulatedReactorOpcUaService` (a `MiloOpcUaClientService` subclass that
serves reads/writes from the plant instead of a socket). This is what the
acceptance test and the runner use:

```bash
# all acceptance bullets (SHADOW, AUTONOMOUS, interlock, override, oscillation)
mvn -q -pl worker test -Dtest=Demo01ReactorCascadeControlTest

# the narrated walk-through, writing /tmp/jneopallium-demo01-audit.jsonl
java -cp "worker/target/classes:$(cat /tmp/cp.txt)" \
  com.rakovpublic.jneuropallium.worker.demo.industrial.Demo01ReactorCascadeControl
```

## 2. Over the wire (real OPC UA) — this directory

Stand up the `asyncua` plant, then point a plain `MiloOpcUaClientService` at
it (no other code changes — the simulated service is the only swap):

```bash
pip install -r requirements.txt
python reactor_plant_server.py            # opc.tcp://localhost:4840/jneopallium/reactor
```

The server integrates the identical FOPDT CSTR model
(`dT/dt = (Q - K·valve·(T - T_cool)) / C`) and exposes:

| Node | Signal tag | Direction |
|---|---|---|
| `ns=2;s=Reactor.TIC101.PV` | `PLANT.TIC101.PV` | read |
| `ns=2;s=Reactor.FIC101.PV` | `PLANT.FIC101.PV` | read |
| `ns=2;s=Reactor.FIC101.OUT` | `PLANT.FIC101.OUT` | write (valve) |
| `ns=2;s=Reactor.HiTempInterlock` | `PLANT.TIC101.HI_ILK` | read |

Config: [`worker/src/test/resources/demo/demo01-reactor.yaml`](../../resources/demo/demo01-reactor.yaml).

For the **read-only smoke test** the demo also mentions, point `endpointUrl`
at `opc.tcp://milo.digitalpetri.com:62541/milo` and keep `writes: []`.
