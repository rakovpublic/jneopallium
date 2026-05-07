# OPC UA bridge — manual demo (S12)

This is the manual smoke-test against the public Eclipse Milo demo
server. It is **not** part of CI — run it by hand before any release.

## Prerequisites

* JDK 17 in `JAVA_HOME`.
* Maven ≥ 3.9.
* Network access to `opc.tcp://milo.digitalpetri.com:62541/milo`.

## Procedure

1. Build the project from a clean checkout:
   ```bash
   mvn -B -U clean verify
   ```
2. Drop a config at `/tmp/opcua-bridge-demo.yaml`:
   ```yaml
   connection:
     endpointUrl: "opc.tcp://milo.digitalpetri.com:62541/milo"
     applicationName: "Jneopallium-OPCUA-Bridge"
     applicationUri: "urn:rakovpublic:jneopallium:bridge:dev01"
     requestTimeout: "PT5S"
     sessionTimeout: "PT2M"
     keepAliveFailuresAllowed: 3
   security:
     policy: NONE
     mode: NONE
     auth:
       type: "Anonymous"
   reads:
     - loopId: "DEMO-CLOCK"
       nodeId: "i=2258"
       signalTag: "DEMO.CLOCK.UTC"
       direction: READ
   writes: []
   alarms: []
   audit:
     localAuditFile: "/tmp/jneopallium-opcua-audit.jsonl"
     writeRejectedToAudit: true
   tickInterval: "PT0.25S"
   ```
3. Start a small Java program (jshell/REPL or a `main`) that:
   ```java
   var cfg = OpcUaBridgeConfigLoader.load(Path.of("/tmp/opcua-bridge-demo.yaml"));
   try (var svc = new MiloOpcUaClientService(cfg)) {
       Thread.sleep(2_000);
       System.out.println("latest = " + svc.latest("DEMO.CLOCK.UTC"));
   }
   ```
4. Within ~2 s the latest-value cache should contain a non-null
   `DataValue` for `Server_ServerStatus_CurrentTime` (NodeId `i=2258`).

## Acceptance

* The bridge connects, lists endpoints, subscribes to one node, and
  receives at least one tick before exit.
* No write happens (writes list is empty).
* No audit records are produced (no commands ran).
* The local audit file at `/tmp/jneopallium-opcua-audit.jsonl` is
  created (or already existed) but contains no new lines.

If any of the above fails, capture the stack trace and the Milo client
log output before filing an issue. Re-run with
`-Dorg.slf4j.simpleLogger.defaultLogLevel=debug` for transport-level
detail.
