# Demo 09: Nengo Interoperability

Story: a local mock Nengo vector stream feeds deterministic vector samples into Jneopallium and receives vector/confidence output.

Network: vector input, feature extraction, temporal smoothing, advisory decision, and result conversion layers.

Safety ceiling: `ADVISORY`. The demo does not require Python Nengo. A real Nengo process can replace the mock stream while preserving the signal and result contract.
