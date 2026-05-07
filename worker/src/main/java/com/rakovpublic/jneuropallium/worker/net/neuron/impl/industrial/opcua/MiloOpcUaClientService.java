/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.identity.UsernameProvider;
import org.eclipse.milo.opcua.sdk.client.subscriptions.MonitoredItemSynchronizationException;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

/**
 * Owns the {@link OpcUaClient}, the single {@link OpcUaSubscription} and
 * the cache of latest {@link DataValue}s. Inputs and the output aggregator
 * share one instance of this service.
 *
 * <p>Reconnect is intentionally simple — Milo's client transparently retries
 * the channel; on permanent failure we surface a high-priority alarm and
 * never silently replay buffered writes.
 */
public class MiloOpcUaClientService implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(MiloOpcUaClientService.class);

    private final OpcUaBridgeConfig cfg;
    private final OpcUaClient client;
    private final OpcUaSubscription subscription;
    private final Map<String, OpcUaNodeBinding> bindingsBySignalTag = new ConcurrentHashMap<>();
    private final Map<NodeId, OpcUaNodeBinding> bindingsByNodeId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, DataValue> latest = new ConcurrentHashMap<>();
    private final BlockingQueue<AlarmEvent> alarmQueue = new LinkedBlockingQueue<>(10_000);

    public MiloOpcUaClientService(OpcUaBridgeConfig cfg) throws UaException {
        this.cfg = Objects.requireNonNull(cfg, "cfg");
        this.client = buildClient(cfg);
        this.client.connect();
        double publishingMillis = Math.max(50, cfg.tickInterval().toMillis());
        this.subscription = new OpcUaSubscription(client, publishingMillis);
        installListener();
        subscription.create();
        registerBindings();
    }

    /** Test-only ctor: bypasses client construction so logic can be unit-tested. */
    protected MiloOpcUaClientService(OpcUaBridgeConfig cfg,
                                     OpcUaClient client,
                                     OpcUaSubscription subscription) {
        this.cfg = cfg;
        this.client = client;
        this.subscription = subscription;
    }

    public OpcUaClient getClient() { return client; }

    public DataValue latest(String signalTag) { return latest.get(signalTag); }

    /** Snapshot of all latest read values keyed by signalTag. */
    public Map<String, DataValue> snapshotLatest() { return Map.copyOf(latest); }

    public OpcUaNodeBinding bindingByNodeId(NodeId id) { return bindingsByNodeId.get(id); }

    public OpcUaNodeBinding bindingBySignalTag(String tag) { return bindingsBySignalTag.get(tag); }

    /**
     * Synchronous write of one value. Returns the per-node {@link StatusCode}
     * the server returned, or {@code null} on transport failure.
     */
    public StatusCode writeValue(NodeId nodeId, DataValue value) {
        try {
            List<StatusCode> results = client.writeValues(List.of(nodeId), List.of(value));
            return results.isEmpty() ? null : results.get(0);
        } catch (UaException e) {
            log.error("OPC UA write failed for nodeId={}: {}", nodeId, e.getMessage());
            return null;
        }
    }

    public List<AlarmEvent> drainAlarms() {
        List<AlarmEvent> out = new ArrayList<>();
        alarmQueue.drainTo(out);
        return out;
    }

    @Override
    public void close() {
        try {
            if (subscription != null) subscription.delete();
        } catch (UaException e) {
            log.warn("Failed to delete subscription: {}", e.getMessage());
        }
        try {
            if (client != null) client.disconnect();
        } catch (UaException e) {
            log.warn("Failed to disconnect client: {}", e.getMessage());
        }
    }

    /* ============================================================ */

    private static OpcUaClient buildClient(OpcUaBridgeConfig cfg) throws UaException {
        IdentityProvider identity = identityProvider(cfg.security().auth());
        return OpcUaClient.create(
                cfg.connection().endpointUrl(),
                endpoints -> endpoints.stream().findFirst(),
                transport -> {},
                builder -> applyClientConfig(cfg, identity, builder));
    }

    private static void applyClientConfig(
            OpcUaBridgeConfig cfg,
            IdentityProvider identity,
            org.eclipse.milo.opcua.sdk.client.OpcUaClientConfigBuilder builder) {
        builder.setApplicationName(
                org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText.english(
                        cfg.connection().applicationName()))
                .setApplicationUri(cfg.connection().applicationUri())
                .setRequestTimeout(unsignedMillis(cfg.connection().requestTimeout().toMillis()))
                .setSessionTimeout(unsignedMillis(cfg.connection().sessionTimeout().toMillis()))
                .setKeepAliveFailuresAllowed(unsigned(cfg.connection().keepAliveFailuresAllowed()))
                .setIdentityProvider(identity);
    }

    private static IdentityProvider identityProvider(OpcUaBridgeConfig.SecurityConfig.Authentication a) {
        if (a instanceof OpcUaBridgeConfig.SecurityConfig.Anonymous) {
            return new AnonymousProvider();
        }
        if (a instanceof OpcUaBridgeConfig.SecurityConfig.UsernamePassword up) {
            String pw = up.passwordEnv() == null ? ""
                    : Optional.ofNullable(System.getenv(up.passwordEnv())).orElse("");
            return new UsernameProvider(up.username() == null ? "" : up.username(), pw);
        }
        if (a instanceof OpcUaBridgeConfig.SecurityConfig.X509) {
            throw new IllegalStateException("X509 identity not yet supported in this bridge build");
        }
        throw new IllegalStateException("Unknown authentication: " + a);
    }

    private static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger unsignedMillis(long ms) {
        return unsigned((int) Math.max(0, Math.min(ms, Integer.MAX_VALUE)));
    }

    private static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger unsigned(int v) {
        return org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint(Math.max(0, v));
    }

    private void installListener() {
        subscription.setSubscriptionListener(new OpcUaSubscription.SubscriptionListener() {
            @Override
            public void onDataReceived(OpcUaSubscription sub,
                                       List<OpcUaMonitoredItem> items,
                                       List<DataValue> values) {
                int n = Math.min(items.size(), values.size());
                for (int i = 0; i < n; i++) {
                    OpcUaMonitoredItem item = items.get(i);
                    DataValue dv = values.get(i);
                    NodeId nid = item.getReadValueId().getNodeId();
                    OpcUaNodeBinding b = bindingsByNodeId.get(nid);
                    if (b == null) continue;
                    latest.put(b.signalTag, dv);
                    b.setLastSeen(dv);
                    if (isAlarmBinding(b)) {
                        if (!alarmQueue.offer(new AlarmEvent(b, dv))) {
                            log.warn("Alarm queue full — dropping alarm for {}", b.signalTag);
                        }
                    }
                }
            }
        });
    }

    private void registerBindings() {
        List<OpcUaMonitoredItem> items = new ArrayList<>();
        Stream.concat(cfg.reads().stream(), cfg.alarms().stream())
                .forEach(c -> {
                    OpcUaNodeBinding b = new OpcUaNodeBinding(c);
                    bindingsByNodeId.put(b.nodeId, b);
                    bindingsBySignalTag.put(b.signalTag, b);
                    OpcUaMonitoredItem mi = OpcUaMonitoredItem.newDataItem(b.nodeId);
                    items.add(mi);
                });
        cfg.writes().forEach(c -> {
            OpcUaNodeBinding b = new OpcUaNodeBinding(c);
            bindingsBySignalTag.put(b.signalTag, b);
            bindingsByNodeId.putIfAbsent(b.nodeId, b);
        });
        if (!items.isEmpty()) {
            subscription.addMonitoredItems(items);
            try {
                subscription.synchronizeMonitoredItems();
            } catch (MonitoredItemSynchronizationException e) {
                e.getCreateResults().forEach(r ->
                        log.error("Failed to create MonitoredItem nodeId={} svc={} op={}",
                                r.monitoredItem().getReadValueId().getNodeId(),
                                r.serviceResult(),
                                r.operationResult().orElse(null)));
                throw new IllegalStateException("Could not subscribe to all bindings", e);
            }
        }
    }

    private boolean isAlarmBinding(OpcUaNodeBinding b) {
        return cfg.alarms().contains(b.config);
    }

    /** A delivered alarm sample — paired with its binding for downstream mapping. */
    public record AlarmEvent(OpcUaNodeBinding binding, DataValue value) {}
}
