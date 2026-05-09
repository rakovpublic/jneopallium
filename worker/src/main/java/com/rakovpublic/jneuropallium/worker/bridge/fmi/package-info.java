/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * FMI/FMU bridge for Jneopallium (03-FMI-FMU.md).
 *
 * <p>The FMI (Functional Mock-up Interface) bridge is the primary testbench
 * for the entire Jneopallium pipeline. Because the "external system" is an
 * FMU file running in the same JVM, this bridge needs no network, no
 * reconnect logic, and no external infrastructure — it can run in CI on a
 * developer laptop end-to-end.
 *
 * <h2>Key classes</h2>
 * <dl>
 *   <dt>{@link com.rakovpublic.jneuropallium.worker.bridge.fmi.FmiBridgeConfig}</dt>
 *   <dd>Immutable configuration record loaded from YAML via
 *       {@link com.rakovpublic.jneuropallium.worker.bridge.fmi.FmiBridgeConfigLoader}.</dd>
 *
 *   <dt>{@link com.rakovpublic.jneuropallium.worker.bridge.fmi.FmuDriver}</dt>
 *   <dd>Abstraction over the FMI C API. Production code would supply a JNA
 *       adapter; tests inject a
 *       {@code StubFmuDriver} that simulates a tank-temperature model in
 *       pure Java.</dd>
 *
 *   <dt>{@link com.rakovpublic.jneuropallium.worker.bridge.fmi.FmuModelDescription}</dt>
 *   <dd>Parsed {@code modelDescription.xml} — maps variable names to FMI
 *       value references.</dd>
 *
 *   <dt>{@link com.rakovpublic.jneuropallium.worker.bridge.fmi.FmuClientService}</dt>
 *   <dd>Lifecycle manager: initialization sequence, per-tick doStep, and
 *       thread-safe variable cache. Supports real-time and
 *       as-fast-as-possible clock modes.</dd>
 *
 *   <dt>{@link com.rakovpublic.jneuropallium.worker.bridge.fmi.FmuMeasurementInput}</dt>
 *   <dd>{@link com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput}
 *       emitting {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal}s
 *       from FMU Real outputs.</dd>
 *
 *   <dt>{@link com.rakovpublic.jneuropallium.worker.bridge.fmi.FmuEventInput}</dt>
 *   <dd>{@code IInitInput} emitting
 *       {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal}s
 *       from FMU Boolean outputs.</dd>
 *
 *   <dt>{@link com.rakovpublic.jneuropallium.worker.bridge.fmi.FmuCommandOutputAggregator}</dt>
 *   <dd>{@link com.rakovpublic.jneuropallium.worker.application.IOutputAggregator}
 *       that enforces the 00-FRAMEWORK §2.2 safety rules, writes setpoints to
 *       FMU Real inputs, then calls {@code doStep} to advance the simulation.</dd>
 * </dl>
 *
 * <h2>Native FMI driver</h2>
 * <p>The {@link com.rakovpublic.jneuropallium.worker.bridge.fmi.FmuDriver}
 * interface intentionally decouples the bridge from any specific native
 * binding library. A JNA or javafmi adapter can be added as a production
 * implementation without touching the bridge logic. See the javafmi reference
 * at {@code https://github.com/CATIA-Systems/JavaFMI}.
 *
 * <h2>Signal mapping (§5)</h2>
 * <pre>
 * FMU Real output    → MeasurementSignal   Quality.GOOD always
 * FMU Boolean output → AlarmSignal         (when true; severity from config)
 * ActuatorCommandSignal → FMU Real input   via aggregator safety chain
 * </pre>
 */
package com.rakovpublic.jneuropallium.worker.bridge.fmi;
