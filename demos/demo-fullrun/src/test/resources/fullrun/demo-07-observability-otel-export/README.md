# Demo 07: OpenTelemetry Export-Only

Story: an OpenTelemetry-like metrics/log/trace stream observes latency, errors, saturation, and failed spans.

Network: metrics/log input, anomaly feature extraction, root-cause candidate, and result conversion layers.

Safety ceiling: `EXPORT-ONLY`. Latency and error spikes produce anomaly summaries with window start/end attributes. No control or writeback result type is emitted.
