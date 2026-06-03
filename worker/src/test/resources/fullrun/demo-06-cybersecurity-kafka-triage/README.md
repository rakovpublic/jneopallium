# Demo 06: Cybersecurity Kafka Triage

Story: a Kafka-like security stream compares attack-like authentication bursts with a benign service-account pattern.

Network: security event input, anomaly and dampening features, threat hypothesis, and result conversion layers.

Safety ceiling: `ADVISORY`. Failed-login bursts raise priority and emit investigation advice, while benign service account noise is dampened. No blocking action is emitted.
