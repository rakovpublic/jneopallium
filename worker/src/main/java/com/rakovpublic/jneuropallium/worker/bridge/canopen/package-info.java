/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * CANopen bridge (13-CANOPEN.md). Read-and-advisory adapter between a
 * CANopen network (CiA-301 / CiA-401 / CiA-402 / CiA-418 device profiles)
 * and the Jneopallium signal pipeline.
 *
 * <p>Structural ceiling per 13-CANOPEN.md §3 is <b>ADVISORY</b>. Writes
 * to the CiA-402 {@code controlword} (0x6040) are rejected at config load
 * unconditionally; every other write must clear a per-(nodeId, odIndex)
 * allow-list at config load and a runtime backstop at send-time. PDO
 * writes go through the universal §2.2 algorithm; SDO writes are slow
 * enough to log every one.
 *
 * <p>Platform support is interface-first
 * ({@link com.rakovpublic.jneuropallium.worker.bridge.canopen.CanopenClientService}):
 * the Linux SocketCAN backend
 * ({@link com.rakovpublic.jneuropallium.worker.bridge.canopen.SocketCanClientService})
 * is the production target; the Lawicel-style USB-CAN backend
 * ({@link com.rakovpublic.jneuropallium.worker.bridge.canopen.UsbCanClientService})
 * is the cross-platform escape hatch.
 */
package com.rakovpublic.jneuropallium.worker.bridge.canopen;
