# Demo 06: Temporal Cybersecurity Threat Correlation

Story: a Kafka-like security stream correlates authentication, process,
DNS, network-flow, asset, threat-intelligence, and maintenance context
for three entities: an ordered attack chain, a benign service-account
maintenance pattern, and a low-and-slow exfiltration pattern.

Network: heterogeneous security input, telemetry normalization,
temporal correlation, advisory investigation action, and result
conversion layers.

Safety ceiling: `ADVISORY`. Ordered evidence raises temporal threat
hypotheses and freezes baseline learning during the attack window.
Maintenance context is a soft gate, not a bypass. No blocking action is
emitted.

Training design: see
`docs/demo-fullrun/cybersecurity-training-design.md` and
`training-data-sources.yaml`.

Trained reference model: see
`worker/src/main/resources/model/cybersecurity-temporal/trained-temporal-threat-model.json`.
