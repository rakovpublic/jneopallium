/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import java.util.Objects;

/** Immutable network 5-tuple used to identify a flow. */
public final class NetworkTuple {
    private final String srcIp;
    private final String dstIp;
    private final String proto;
    private final int srcPort;
    private final int dstPort;

    public NetworkTuple(String srcIp, String dstIp, String proto, int srcPort, int dstPort) {
        this.srcIp = srcIp;
        this.dstIp = dstIp;
        this.proto = proto;
        this.srcPort = srcPort;
        this.dstPort = dstPort;
    }

    public String getSrcIp() { return srcIp; }
    public String getDstIp() { return dstIp; }
    public String getProto() { return proto; }
    public int getSrcPort() { return srcPort; }
    public int getDstPort() { return dstPort; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NetworkTuple)) return false;
        NetworkTuple n = (NetworkTuple) o;
        return srcPort == n.srcPort && dstPort == n.dstPort
                && Objects.equals(srcIp, n.srcIp)
                && Objects.equals(dstIp, n.dstIp)
                && Objects.equals(proto, n.proto);
    }

    @Override
    public int hashCode() { return Objects.hash(srcIp, dstIp, proto, srcPort, dstPort); }

    @Override
    public String toString() {
        return (proto == null ? "?" : proto) + " " + srcIp + ":" + srcPort + "->" + dstIp + ":" + dstPort;
    }
}
