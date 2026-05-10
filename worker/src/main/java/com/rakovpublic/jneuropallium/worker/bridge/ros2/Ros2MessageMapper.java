/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.ros2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rakovpublic.jneuropallium.ai.signals.fast.MotorCommandSignal;
import com.rakovpublic.jneuropallium.ai.signals.fast.SensorySignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.ProprioceptiveSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.EfficiencySignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.PeerObservationSignal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure functions: ROS 2 JSON message ↔ Jneopallium typed signal
 * (04-ROS2-DDS.md §5).
 *
 * <p>Mapping is deliberately minimal. Per §5 a large payload (image,
 * point-cloud) is carried as a reference (URI / sha) rather than bytes; only
 * the {@link SensorySignal} {@code rawValues} is populated for compact
 * messages such as {@code LaserScan}. {@link Ros2ClientService} applies
 * the per-binding decimation and payload caps ({@code decimateBy},
 * {@code maxPayloadBytes}, §7 / §10 R3) before calling this mapper.
 */
public final class Ros2MessageMapper {

    private static final ObjectMapper JSON =
            new ObjectMapper().disable(SerializationFeature.INDENT_OUTPUT);

    /** Returned to {@link #toSignal} on a malformed payload. The bridge audits. */
    public static final class SignalMapperException extends RuntimeException {
        public SignalMapperException(String message) { super(message); }
        public SignalMapperException(String message, Throwable cause) { super(message, cause); }
    }

    public ObjectMapper jsonMapper() { return JSON; }

    /**
     * Decode a rosbridge {@code "publish"} envelope and route the inner
     * {@code msg} field through {@link #toSignal(Ros2TopicBinding, JsonNode)}.
     * The envelope shape is
     * {@code { "op":"publish", "topic":"…", "msg":{ … } }}.
     */
    public IInputSignal fromRosbridgeEnvelope(Ros2TopicBinding b, String envelopeJson) {
        try {
            JsonNode root = JSON.readTree(envelopeJson);
            JsonNode msg = root.get("msg");
            if (msg == null || msg.isNull()) return null;
            return toSignal(b, msg);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new SignalMapperException("malformed JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Map a decoded ROS 2 message body to the Jneopallium signal class
     * implied by the binding. {@code null} when the message carries no
     * actionable value (e.g. a header-only ping).
     */
    public IInputSignal toSignal(Ros2TopicBinding b, JsonNode msg) {
        if (msg == null) return null;
        long ts = headerTimestampMs(msg);
        if (b.asPeerObservation() && "nav_msgs/msg/Odometry".equals(b.msgType())) {
            return toPeerObservation(b, msg);
        }
        return switch (b.msgType()) {
            case "nav_msgs/msg/Odometry" -> toOdomProprioceptive(b, msg, ts);
            case "sensor_msgs/msg/JointState" -> toJointStateProprioceptive(b, msg, ts);
            case "sensor_msgs/msg/LaserScan" -> toLaserScanSensory(b, msg, ts);
            case "sensor_msgs/msg/Image",
                 "sensor_msgs/msg/CompressedImage" -> toImageReferenceSensory(b, msg, ts);
            case "sensor_msgs/msg/BatteryState" -> toBatteryEfficiency(b, msg);
            case "turtlesim/msg/Pose" -> toTurtlePoseProprioceptive(b, msg, ts);
            default -> toGenericSensory(b, msg, ts);
        };
    }

    /* ===== egress encoders ============================================== */

    /** Encode a {@code geometry_msgs/msg/Twist} from a {@link MotorCommandSignal}. */
    public String encodeTwist(MotorCommandSignal cmd) {
        double[] p = cmd == null ? null : cmd.getParams();
        double linX = (p != null && p.length > 0) ? p[0] : 0.0;
        double linY = (p != null && p.length > 1) ? p[1] : 0.0;
        double linZ = (p != null && p.length > 2) ? p[2] : 0.0;
        double angX = (p != null && p.length > 3) ? p[3] : 0.0;
        double angY = (p != null && p.length > 4) ? p[4] : 0.0;
        double angZ = (p != null && p.length > 5) ? p[5] : 0.0;
        ObjectNode root = JSON.createObjectNode();
        ObjectNode lin = root.putObject("linear");
        lin.put("x", linX); lin.put("y", linY); lin.put("z", linZ);
        ObjectNode ang = root.putObject("angular");
        ang.put("x", angX); ang.put("y", angY); ang.put("z", angZ);
        return root.toString();
    }

    /** Encode a {@code std_msgs/msg/String}. */
    public String encodeStdString(String text) {
        ObjectNode root = JSON.createObjectNode();
        root.put("data", text == null ? "" : text);
        return root.toString();
    }

    /** Encode a {@code std_msgs/msg/Float64}. */
    public String encodeStdFloat64(double value) {
        ObjectNode root = JSON.createObjectNode();
        root.put("data", value);
        return root.toString();
    }

    /* ===== ingress decoders ============================================ */

    private IInputSignal toOdomProprioceptive(Ros2TopicBinding b, JsonNode msg, long ts) {
        // /<topic>/odom: pose.pose.position.{x,y,z} + twist.twist.linear.{x,y,z}
        double px = readDouble(msg, "pose", "pose", "position", "x");
        double py = readDouble(msg, "pose", "pose", "position", "y");
        double pz = readDouble(msg, "pose", "pose", "position", "z");
        double vx = readDouble(msg, "twist", "twist", "linear", "x");
        double vy = readDouble(msg, "twist", "twist", "linear", "y");
        double vz = readDouble(msg, "twist", "twist", "linear", "z");
        ProprioceptiveSignal sig = new ProprioceptiveSignal(
                b.bindingId().hashCode(), new double[]{px, py, pz, vx, vy, vz}, ts);
        sig.setName(b.signalTag());
        return sig;
    }

    private IInputSignal toJointStateProprioceptive(Ros2TopicBinding b, JsonNode msg, long ts) {
        // {name: [...], position: [...], velocity: [...], effort: [...]}
        double[] positions = readDoubleArray(msg, "position");
        ProprioceptiveSignal sig = new ProprioceptiveSignal(
                b.bindingId().hashCode(), positions, ts);
        sig.setName(b.signalTag());
        return sig;
    }

    private IInputSignal toLaserScanSensory(Ros2TopicBinding b, JsonNode msg, long ts) {
        // {ranges: [...]} — already capped upstream by maxRangeBins
        double[] ranges = readDoubleArray(msg, "ranges");
        if (b.maxRangeBins() > 0 && ranges.length > b.maxRangeBins()) {
            double[] capped = new double[b.maxRangeBins()];
            int step = ranges.length / b.maxRangeBins();
            for (int i = 0; i < b.maxRangeBins(); i++) capped[i] = ranges[i * step];
            ranges = capped;
        }
        SensorySignal sig = new SensorySignal(ranges, b.signalTag(), ts);
        return sig;
    }

    private IInputSignal toImageReferenceSensory(Ros2TopicBinding b, JsonNode msg, long ts) {
        // §5: do NOT carry image bytes through the bus. Hash the data field for a stable reference.
        JsonNode dataNode = msg.get("data");
        long contentRef = dataNode == null ? 0L : (long) dataNode.toString().hashCode();
        SensorySignal sig = new SensorySignal(new double[]{(double) contentRef}, b.signalTag(), ts);
        return sig;
    }

    private IInputSignal toBatteryEfficiency(Ros2TopicBinding b, JsonNode msg) {
        // sensor_msgs/msg/BatteryState: percentage in [0,1]; voltage etc. ignored at v1
        double pct = readDouble(msg, "percentage");
        // Map 0..1 percentage to efficiency in [0,1] vs baseline 1.0.
        EfficiencySignal sig = new EfficiencySignal(b.signalTag(), pct, 1.0);
        return sig;
    }

    private IInputSignal toPeerObservation(Ros2TopicBinding b, JsonNode msg) {
        double px = readDouble(msg, "pose", "pose", "position", "x");
        double py = readDouble(msg, "pose", "pose", "position", "y");
        double pz = readDouble(msg, "pose", "pose", "position", "z");
        double vx = readDouble(msg, "twist", "twist", "linear", "x");
        double vy = readDouble(msg, "twist", "twist", "linear", "y");
        double vz = readDouble(msg, "twist", "twist", "linear", "z");
        String peerId = b.peerId() == null ? b.bindingId() : b.peerId();
        PeerObservationSignal sig = new PeerObservationSignal(
                peerId, new double[]{px, py, pz}, new double[]{vx, vy, vz}, 1.0);
        sig.setName(b.signalTag());
        return sig;
    }

    private IInputSignal toTurtlePoseProprioceptive(Ros2TopicBinding b, JsonNode msg, long ts) {
        // turtlesim/msg/Pose: x,y,theta,linear_velocity,angular_velocity
        double x = readDouble(msg, "x");
        double y = readDouble(msg, "y");
        double theta = readDouble(msg, "theta");
        double lv = readDouble(msg, "linear_velocity");
        double av = readDouble(msg, "angular_velocity");
        ProprioceptiveSignal sig = new ProprioceptiveSignal(
                b.bindingId().hashCode(), new double[]{x, y, theta, lv, av}, ts);
        sig.setName(b.signalTag());
        return sig;
    }

    private IInputSignal toGenericSensory(Ros2TopicBinding b, JsonNode msg, long ts) {
        // Best-effort: pick a "data" scalar if present, else hash the payload.
        JsonNode data = msg.get("data");
        double v;
        if (data != null && data.isNumber()) {
            v = data.doubleValue();
        } else if (data != null && data.isBoolean()) {
            v = data.booleanValue() ? 1.0 : 0.0;
        } else {
            v = (double) msg.toString().hashCode();
        }
        return new SensorySignal(new double[]{v}, b.signalTag(), ts);
    }

    /* ===== helpers ===================================================== */

    private static double readDouble(JsonNode root, String... path) {
        JsonNode node = root;
        for (String p : path) {
            if (node == null) return 0.0;
            node = node.get(p);
        }
        return node == null || node.isNull() || !node.isNumber() ? 0.0 : node.doubleValue();
    }

    private static double[] readDoubleArray(JsonNode root, String key) {
        JsonNode arr = root == null ? null : root.get(key);
        if (arr == null || !arr.isArray()) return new double[0];
        List<Double> out = new ArrayList<>(arr.size());
        for (JsonNode n : arr) {
            if (n.isNumber()) out.add(n.doubleValue());
            else if (n.isNull()) out.add(0.0);
        }
        double[] result = new double[out.size()];
        for (int i = 0; i < out.size(); i++) result[i] = out.get(i);
        return result;
    }

    /**
     * §0.6: prefer the message's {@code header.stamp} (sec, nanosec) over
     * the JVM clock. Falls back to {@link System#currentTimeMillis()}.
     */
    private static long headerTimestampMs(JsonNode msg) {
        JsonNode header = msg.get("header");
        if (header == null) return System.currentTimeMillis();
        JsonNode stamp = header.get("stamp");
        if (stamp == null) return System.currentTimeMillis();
        long sec = stamp.has("sec") && stamp.get("sec").isNumber() ? stamp.get("sec").longValue() : 0L;
        long nsec = stamp.has("nanosec") && stamp.get("nanosec").isNumber()
                ? stamp.get("nanosec").longValue() : 0L;
        if (sec == 0 && nsec == 0) return System.currentTimeMillis();
        return sec * 1000L + nsec / 1_000_000L;
    }

    /** Build a rosbridge {@code subscribe} command envelope. */
    public String rosbridgeSubscribe(String topic, String msgType) {
        Map<String, Object> cmd = new LinkedHashMap<>();
        cmd.put("op", "subscribe");
        cmd.put("topic", topic);
        cmd.put("type", msgType);
        return writeJson(cmd);
    }

    /** Build a rosbridge {@code advertise} command envelope. */
    public String rosbridgeAdvertise(String topic, String msgType) {
        Map<String, Object> cmd = new LinkedHashMap<>();
        cmd.put("op", "advertise");
        cmd.put("topic", topic);
        cmd.put("type", msgType);
        return writeJson(cmd);
    }

    /** Build a rosbridge {@code publish} envelope. {@code msgJson} is the inner ROS message JSON. */
    public String rosbridgePublishEnvelope(String topic, String msgJson) {
        try {
            ObjectNode root = JSON.createObjectNode();
            root.put("op", "publish");
            root.put("topic", topic);
            root.set("msg", JSON.readTree(msgJson));
            return root.toString();
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new SignalMapperException("publish envelope encode failed: " + e.getMessage(), e);
        }
    }

    private String writeJson(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new SignalMapperException("json encode failed: " + e.getMessage(), e);
        }
    }
}
