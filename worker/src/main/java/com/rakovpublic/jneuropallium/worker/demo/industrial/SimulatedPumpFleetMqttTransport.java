/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.demo.industrial;

import com.rakovpublic.jneuropallium.worker.bridge.mqtt.MqttTransport;
import org.eclipse.tahu.message.SparkplugBPayloadEncoder;
import org.eclipse.tahu.message.model.Metric;
import org.eclipse.tahu.message.model.MetricDataType;
import org.eclipse.tahu.message.model.SparkplugBPayload;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * In-process {@link MqttTransport} that loops Sparkplug B publishes from a
 * {@link PumpFleetSimulator} back into the MQTT bridge (no broker required).
 *
 * <p>Equivalent in role to {@link SimulatedReactorOpcUaService} for demo-01:
 * the entire production bridge code path —
 * {@code MqttClientService}, {@code MqttSignalMapper},
 * {@code MqttMetricInput}, {@code MqttAdvisoryOutputAggregator},
 * {@code MqttAuditOutput} — runs unchanged; only the network is stubbed.
 *
 * <p>Use {@link #emitDbirthAll}, {@link #emitDdataTick}, {@link #emitDdeath}
 * to feed the simulator into the bridge subscriptions. Outbound
 * advisory publishes are captured in {@link #publishesTo(String)} for the
 * runner / acceptance test to assert on.
 */
public final class SimulatedPumpFleetMqttTransport implements MqttTransport {

    public record Published(String topic, byte[] payload, int qos, boolean retain) {}

    private final PumpFleetSimulator simulator;
    private final String groupId;
    private final String edgeNodeId;

    private final List<String> subscriptions = new ArrayList<>();
    private final Map<String, List<Published>> publishedByTopic = new LinkedHashMap<>();
    private final List<Published> publishedAll = new ArrayList<>();

    private MessageHandler handler;
    private volatile boolean connected;
    private int failNextPublishes;

    public SimulatedPumpFleetMqttTransport(PumpFleetSimulator simulator,
                                           String groupId,
                                           String edgeNodeId) {
        this.simulator = Objects.requireNonNull(simulator, "simulator");
        this.groupId = Objects.requireNonNull(groupId, "groupId");
        this.edgeNodeId = Objects.requireNonNull(edgeNodeId, "edgeNodeId");
    }

    /* ===== MqttTransport ====================================================== */

    @Override public synchronized void connect() { connected = true; }
    @Override public synchronized void setHandler(MessageHandler h) { this.handler = h; }
    @Override public synchronized boolean isConnected() { return connected; }

    @Override
    public synchronized void subscribe(String filter, int qos) {
        subscriptions.add(filter);
    }

    @Override
    public synchronized void publish(String topic, byte[] payload, int qos, boolean retain) {
        if (failNextPublishes > 0) {
            failNextPublishes--;
            throw new MqttTransportException("simulated publish failure on " + topic);
        }
        publishedByTopic.computeIfAbsent(topic, k -> new ArrayList<>())
                .add(new Published(topic, payload, qos, retain));
        publishedAll.add(new Published(topic, payload, qos, retain));
    }

    @Override
    public synchronized void close() { connected = false; }

    /* ===== Simulator feed ===================================================== */

    /** Encode and self-deliver a DBIRTH for every pump using its current state. */
    public synchronized void emitDbirthAll() {
        for (String pumpId : simulator.pumpIds()) {
            emitMessage("DBIRTH", pumpId,
                    simulator.vibration(pumpId),
                    simulator.bearingTemp(pumpId));
        }
    }

    /** Advance the simulator one tick and emit a DDATA message per pump. */
    public synchronized void emitDdataTick(double dtSeconds) {
        simulator.step(dtSeconds);
        for (String pumpId : simulator.pumpIds()) {
            emitMessage("DDATA", pumpId,
                    simulator.vibration(pumpId),
                    simulator.bearingTemp(pumpId));
        }
    }

    /** Emit a DDEATH for one pump — the bridge should raise DEVICE_OFFLINE. */
    public synchronized void emitDdeath(String pumpId) {
        deliver("spBv1.0/" + groupId + "/DDEATH/" + edgeNodeId + "/" + pumpId, new byte[0]);
    }

    /** Make the next {@code n} {@link #publish} calls throw — used to drive PUBLISH_ERROR audits. */
    public synchronized void failNextPublishes(int n) { this.failNextPublishes = Math.max(0, n); }

    /* ===== Inspection helpers ================================================= */

    /** All publishes ever observed on {@code topic}, in send order. */
    public synchronized List<Published> publishesTo(String topic) {
        return new ArrayList<>(publishedByTopic.getOrDefault(topic, List.of()));
    }

    /** Every publish observed, in send order. */
    public synchronized List<Published> allPublishes() { return new ArrayList<>(publishedAll); }

    /** Current number of registered subscriptions (mostly for debug). */
    public synchronized int subscriptionCount() { return subscriptions.size(); }

    /* ===== internal =========================================================== */

    private void emitMessage(String type, String pumpId, double vib, double temp) {
        try {
            Date now = new Date();
            SparkplugBPayload payload = new SparkplugBPayload(now, new ArrayList<>());
            payload.addMetric(new Metric(Demo02Config.METRIC_VIBRATION, null, now,
                    MetricDataType.Double, false, false, null, null, vib));
            payload.addMetric(new Metric(Demo02Config.METRIC_TEMP, null, now,
                    MetricDataType.Double, false, false, null, null, temp));
            byte[] bytes = new SparkplugBPayloadEncoder().getBytes(payload, false);
            String topic = "spBv1.0/" + groupId + "/" + type + "/" + edgeNodeId + "/" + pumpId;
            deliver(topic, bytes);
        } catch (Exception e) {
            throw new MqttTransportException("simulated encode/deliver failed: " + e.getMessage(), e);
        }
    }

    private void deliver(String topic, byte[] payload) {
        MessageHandler h = this.handler;
        if (h == null) return;
        for (String filter : subscriptions) {
            if (matches(filter, topic)) {
                h.onMessage(new InboundMessage(topic, payload));
                return;
            }
        }
    }

    static boolean matches(String filter, String topic) {
        StringBuilder rx = new StringBuilder("^");
        String[] parts = filter.split("/", -1);
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (i > 0) rx.append('/');
            switch (p) {
                case "+" -> rx.append("[^/]+");
                case "#" -> {
                    rx.append(".*");
                    return Pattern.compile(rx.toString()).matcher(topic).matches();
                }
                default -> rx.append(Pattern.quote(p));
            }
        }
        rx.append('$');
        return Pattern.compile(rx.toString()).matcher(topic).matches();
    }
}
