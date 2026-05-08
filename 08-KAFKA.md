# Bridge 08 — Apache Kafka (enterprise event streaming / cybersecurity / telemetry)

> **Prerequisite:** `00-FRAMEWORK.md`.

**Priority:** high. **Safety ceiling:** `ADVISORY`.

## 1. Domain context

[Apache Kafka](https://kafka.apache.org/) is the dominant distributed event-streaming platform — high-throughput, durable, partitioned. Most enterprises run Kafka clusters carrying logs, network packets, alerts, user-activity events, and machine telemetry.

The repo's `security/` package already has every signal type this bridge produces: `LogEventSignal`, `PacketSignal`, `AnomalyScoreSignal`, `IncidentReportSignal`, `SignatureMatchSignal`, `ThreatHypothesisSignal`, `SyscallSignal`, `QuarantineRequestSignal`, `QuarantineLiftSignal`, `SelfToleranceSignal`, `InflammationBroadcastSignal`. The bridge is therefore a Kafka-consumer mapper feeding the cybersecurity module, plus a Kafka-producer aggregator emitting advisory incident topics.

There is also an **existing** `KafkaInitInput` in `worker/.../net/signals/storage/kafka/` — that's a generic Kafka source for the framework's own signal serialization, **not** an event-typed bridge. Don't reuse it directly; the `Kafka<X>Input` classes in this bridge produce typed security signals from heterogeneous payloads (Logstash JSON, Zeek, Suricata, OS telemetry).

## 2. Maven dependencies

```xml
<!-- kafka-clients is already in worker/pom.xml at 3.9.2 -->
<!-- Schema registry client (optional — for Avro/Protobuf-encoded topics) -->
<dependency>
    <groupId>io.confluent</groupId>
    <artifactId>kafka-schema-registry-client</artifactId>
    <version>7.7.1</version>
</dependency>
<!-- For payload schemas -->
<dependency>
    <groupId>io.confluent</groupId>
    <artifactId>kafka-avro-serializer</artifactId>
    <version>7.7.1</version>
</dependency>
```

If your deployment doesn't use Confluent Schema Registry, omit those two and bind topics to JSON-only payloads.

## 3. Architecture

```
┌──────────────────────┐                          ┌────────────────────────┐
│ Kafka cluster        │      consumer.poll()     │ KafkaClientService     │
│  topics:             │ ───────────────────────▶ │  • per-binding consumer│
│   - logs.app         │                          │  • offset checkpoint   │
│   - logs.security    │ ◀───── producer.send() ──│  • payload deserialise │
│   - net.zeek.conn    │                          │  • producer for advisory│
│   - net.suricata.alt │                          │    topics              │
│   - host.syscalls    │                          └─────┬───────────┬──────┘
│   - jneo.advisory.*  │                                │           │
└──────────────────────┘                       ┌────────▼─┐  ┌──────▼───────┐
                                               │ KafkaLog │  │ KafkaPacket  │
                                               │ Input    │  │ Input        │
                                               │ → LogEvt │  │ → PacketSig  │
                                               │ → SigMtch│  │ → SyscallSig │
                                               └──────────┘  └──────────────┘
                                                              ▼
                              [Pipeline → KafkaAdvisoryOutputAggregator]
                                                              ▼
                              produce IncidentReport / QuarantineRequest /
                              ThreatHypothesis to advisory topics that
                              SOC tooling subscribes to
```

## 4. Signal mapping

All target signals already exist; the bridge's value is in the per-source decoders.

| Source topic (typical) | Payload format | Jneopallium signal |
|---|---|---|
| `logs.security` (auth, privilege, failure) | JSON Logstash | `LogEventSignal` (+ `SignatureMatchSignal` if a known regex hits) |
| `net.zeek.conn` | JSON Zeek | `PacketSignal` (flow-summary level, not packet-level) |
| `net.suricata.alert` | JSON EVE | `SignatureMatchSignal` + optional `ThreatHypothesisSignal` |
| `host.syscalls.osquery` | JSON osquery | `SyscallSignal` |
| `endpoint.scores` | Avro / JSON, scored by an upstream ML model | `AnomalyScoreSignal` |

Egress (advisory):

| Jneopallium signal | Producer topic |
|---|---|
| `IncidentReportSignal` | `jneo.advisory.incidents` |
| `QuarantineRequestSignal` | `jneo.advisory.quarantine_requests` (consumed by EDR/SOAR — never directly executed) |
| `ThreatHypothesisSignal` | `jneo.advisory.hypotheses` |
| `TransparencyLogSignal` | `jneo.advisory.audit` |

## 5. Configuration

```yaml
cluster:
  bootstrapServers: "kafka01.sec.local:9093,kafka02.sec.local:9093"
  consumerGroupId: "jneopallium-bridge-prod"
  enableAutoCommit: false
  maxPollRecords: 500

security:
  protocol: "SASL_SSL"
  saslMechanism: "OAUTHBEARER"            # or "PLAIN", "SCRAM-SHA-512"
  truststore: "/etc/jneopallium/kafka-truststore.jks"
  oauthTokenEndpoint: "https://idp.sec.local/oauth2/token"
  clientIdEnv: "KAFKA_CLIENT_ID"
  clientSecretEnv: "KAFKA_CLIENT_SECRET"

schemaRegistry:
  enabled: true
  url: "https://schemas.sec.local"

reads:
  - bindingId: "AUTH-LOGS"
    topic: "logs.security"
    payloadFormat: "JSON"
    decoder: "LOGSTASH"
    targetSignal: "LOG_EVENT"
    signalTagPrefix: "SEC.AUTH"

  - bindingId: "ZEEK-CONN"
    topic: "net.zeek.conn"
    payloadFormat: "JSON"
    decoder: "ZEEK_CONN"
    targetSignal: "PACKET"
    signalTagPrefix: "NET.CONN"

  - bindingId: "SURICATA-ALERTS"
    topic: "net.suricata.alert"
    payloadFormat: "JSON"
    decoder: "SURICATA_EVE"
    targetSignal: "SIGNATURE_MATCH"
    signalTagPrefix: "NET.IDS"

writes:
  - bindingId: "INCIDENTS"
    topic: "jneo.advisory.incidents"
    payloadFormat: "JSON"
    signalTag: "SEC.INCIDENT"

audit:
  localAuditFile: "/var/log/jneopallium/kafka-audit.jsonl"

perTagSafetyMode:
  INCIDENTS: ADVISORY    # AUTONOMOUS rejected by config validator
```

## 6. Package layout

```
worker/src/main/java/com/rakovpublic/jneuropallium/worker/bridge/kafka/
├── KafkaBridgeConfig.java
├── KafkaBridgeConfigLoader.java
├── KafkaTopicBinding.java
├── KafkaSignalMapper.java
├── decoder/
│   ├── PayloadDecoder.java          (interface)
│   ├── LogstashJsonDecoder.java
│   ├── ZeekConnDecoder.java
│   ├── SuricataEveDecoder.java
│   ├── OsqueryDecoder.java
│   └── AvroSchemaRegistryDecoder.java
├── KafkaClientService.java
├── KafkaLogInput.java
├── KafkaNetworkInput.java
├── KafkaSyscallInput.java
└── KafkaAdvisoryOutputAggregator.java
```

The `decoder/` subpackage is the bridge's main extension point — adding a new source format means writing a new `PayloadDecoder`.

## 7. Phase plan

| Phase | Goal |
|-------|------|
| 1 | Read-only consumers; manual offset commit; LOGSTASH + ZEEK_CONN + SURICATA_EVE decoders. |
| 2 | Producer for advisory topics. SOAR/EDR integration is out of scope, but the topic schema is documented. |
| 3 | **Not autonomous.** Quarantine requests are consumed by the SOAR, which gates them. |

## 8. Bridge-specific scenarios

| # | Scenario | Setup | Expected |
|---|----------|-------|----------|
| **S7** | Throughput | Test producer at 10k msg/s on `logs.security` | Bridge keeps up; lag stays under one batch cycle; no rebalances |
| **S8** | Decoder failure isolation | Inject malformed JSON | One audit `verdict=FAILED reason=DECODER_ERROR`; consumer offset NOT advanced past the bad record (configurable) |
| **S9** | At-least-once semantics | Crash bridge mid-batch | On restart, all unacked messages re-delivered; no signal silently lost |
| **S10** | Schema registry evolution | Avro schema adds an optional field | Bridge keeps working; old schema still decodes correctly |
| **S11** | Producer to advisory topic | Pipeline emits `IncidentReportSignal` | Producer publishes to `jneo.advisory.incidents`; audit `verdict=APPLIED` |
| **S12** | Consumer group rebalance | Add a second bridge instance to the group | Partitions redistributed cleanly; no signals duplicated |

## 9. Risks

| # | Risk | Mitigation |
|---|------|------------|
| R1 | Bad decoder advances offset → lost events | Default `failurePolicy: STOP_AT_FAILED_OFFSET`; opt-in `SKIP_AND_LOG`. |
| R2 | Schema registry unavailable | Bridge degrades to JSON-only mode if SR was optional; fails fast if SR was mandatory. |
| R3 | Consumer lag during cybersecurity incident | Per-binding `maxPollIntervalMs` and dedicated thread per binding; expose lag as a metric for OpenTelemetry bridge to consume. |
| R4 | Producer buffer overrun on advisory output | Bounded queue; on overflow drop oldest with `verdict=FAILED reason=ADVISORY_QUEUE_FULL`. |

## 10. References

* Kafka — `https://kafka.apache.org/`.
* Confluent Schema Registry — `https://docs.confluent.io/platform/current/schema-registry/`.
* Suricata EVE JSON — `https://docs.suricata.io/en/latest/output/eve/eve-json-format.html`.
* Zeek conn log — `https://docs.zeek.org/en/master/logs/conn.html`.
