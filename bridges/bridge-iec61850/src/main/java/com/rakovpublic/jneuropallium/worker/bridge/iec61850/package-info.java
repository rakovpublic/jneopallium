/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * IEC 61850 bridge — Bridge 11 (11-IEC61850.md).
 *
 * <p>Adapter that converts substation measurements, statuses and events
 * produced by IEDs (protection relays, merging units, breaker controls)
 * — surfaced through IEC 61850 MMS reads and report-control subscriptions
 * — into {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal},
 * {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.InterlockSignal}
 * and
 * {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal}
 * instances that the Jneopallium pipeline can integrate with state
 * estimation, anomaly detection and equipment-degradation modelling.
 *
 * <p><b>Bridge ceiling: READ-ONLY — initially.</b> This package contains no
 * aggregator / output / write class (11-IEC61850.md §3, §4 diagram, §7
 * "No aggregator class"). The {@link com.rakovpublic.jneuropallium.worker.bridge.iec61850.Iec61850MmsClient}
 * seam exposes only read primitives — no method writes a Data Attribute,
 * issues a select-before-operate, or controls a breaker. Substation
 * control writes are a SIL-classified function that belongs on certified
 * RTUs and protection relays; a Jneopallium bridge writing to an
 * {@code Oper} attribute would act outside its certification scope
 * (11-IEC61850.md §3).
 *
 * <p>A {@code writes:} block in the bridge YAML is rejected at
 * config-load with a clear message
 * ({@link com.rakovpublic.jneuropallium.worker.bridge.iec61850.Iec61850BridgeConfig#create}).
 *
 * <p>Acceptance tests use {@link com.rakovpublic.jneuropallium.worker.bridge.iec61850.InMemoryIec61850MmsClient}
 * to simulate an IED without depending on a native MMS stack. A production
 * adapter against {@code org.openmuc:iec61850bean} (or the JNI bindings of
 * {@code libiec61850}) implements the same seam and slots in unchanged
 * (11-IEC61850.md §2).
 */
package com.rakovpublic.jneuropallium.worker.bridge.iec61850;
