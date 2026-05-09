/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * Apache PLC4X bridge (01-PLC4X.md).
 *
 * <p>Adapter between legacy PLC fieldbus protocols (Siemens S7, Modbus TCP,
 * EtherNet/IP, Beckhoff ADS, …) and the Jneopallium signal pipeline. Implements
 * the universal contract from 00-FRAMEWORK.md (§0 ground rules, §2.2 write
 * algorithm, §4 audit schema).
 *
 * <p>Unlike OPC UA, most PLC4X drivers do not expose a native subscription
 * mechanism — reads are <b>polled</b> at a per-binding rate by
 * {@link com.rakovpublic.jneuropallium.worker.bridge.plc4x.Plc4xClientService},
 * which keeps a latest-value cache that the input adapters snapshot on each
 * tick.
 *
 * <p>The bridge talks to the field through the {@link
 * com.rakovpublic.jneuropallium.worker.bridge.plc4x.Plc4xDriver} abstraction so
 * tests can inject an in-memory simulator without dragging the real PLC4X
 * jars or a live PLC into the test classpath. A production wiring binds the
 * abstraction to {@code org.apache.plc4x.java.api.PlcDriverManager} +
 * {@code PlcConnection}.
 */
package com.rakovpublic.jneuropallium.worker.bridge.plc4x;
