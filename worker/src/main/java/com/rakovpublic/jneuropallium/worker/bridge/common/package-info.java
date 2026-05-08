/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * Shared scaffolding for every Jneopallium bridge ({@code 00-FRAMEWORK.md}).
 *
 * <p>The framework treats a "bridge" as a typed adapter between an external
 * real-world system and the Jneopallium signal pipeline. Every bridge —
 * OPC UA, PLC4X, MQTT, FMI, ROS 2, LSL, FHIR, DICOM, Kafka, OpenTelemetry,
 * Ditto, IEC 61850, MAVLink, CANopen, LTI/xAPI — implements the same
 * contract:
 *
 * <ol>
 *   <li>Reads are pulled from a connection-service cache and converted to
 *       typed {@link com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal}s
 *       by a {@code <Bridge>MeasurementInput} implementing
 *       {@link com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput}.</li>
 *   <li>Writes flow through a {@code <Bridge>CommandOutputAggregator}
 *       implementing
 *       {@link com.rakovpublic.jneuropallium.worker.application.IOutputAggregator}.
 *       Writes follow the universal §2.2 algorithm — extend
 *       {@link com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeOutputAggregator}
 *       to inherit it.</li>
 *   <li>Every write produces an audit record (§4) — see
 *       {@link com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord}
 *       and {@link com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput}.</li>
 * </ol>
 *
 * <p>The six ground rules from §0 of the framework spec are enforced
 * structurally: interlocks have direct authority (no vetoes apply),
 * operator overrides win for regulatory control, every write produces an
 * audit record, source quality propagates, and source timestamps are used
 * verbatim.
 *
 * <p>The OPC UA bridge predates this convention and lives at
 * {@code worker/net/neuron/impl/industrial/opcua/}. New bridges go in
 * {@code worker.bridge.<id>} per §1.
 */
package com.rakovpublic.jneuropallium.worker.bridge.common;
