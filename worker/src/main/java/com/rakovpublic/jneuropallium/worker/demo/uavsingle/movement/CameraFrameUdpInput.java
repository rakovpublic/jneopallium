package com.rakovpublic.jneuropallium.worker.demo.uavsingle.movement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.demo.uavsingle.CameraFrameSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Camera-channel {@link IInitInput} for the live run. The thin CARLA-Air bridge publishes one JSON
 * datagram per FPV frame — a downsampled grayscale pixel matrix plus the public perception/search
 * context (nearest-target bearing, search bounds, counts) — and this input decodes it into a
 * {@link CameraFrameSignal} that the kept {@code CameraFramePatchInitInput} + recognition layers
 * consume. The perception/search context rides along in the signal's attributes so the runner can
 * assemble the {@link MovementObservationSignal} without the policy ever touching the socket.
 *
 * <p>Frames travel on their own channel (not MAVLink) because the MAVLink flight bus must not carry
 * images (12-MAVLINK.md §5); a ROS2 image topic is the production-grade alternative.
 */
public final class CameraFrameUdpInput implements IInitInput {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String name;
    private final int bindPort;
    private final ConcurrentLinkedQueue<CameraFrameSignal> frames = new ConcurrentLinkedQueue<>();

    private volatile DatagramSocket socket;
    private volatile boolean running;
    private Thread receiver;

    public CameraFrameUdpInput(String name, int bindPort) {
        this.name = name == null ? "uav-camera-udp-input" : name;
        this.bindPort = bindPort;
    }

    public void start() {
        if (running) {
            return;
        }
        try {
            socket = new DatagramSocket(bindPort);
        } catch (IOException e) {
            throw new IllegalStateException("Camera UDP bind failed on port " + bindPort, e);
        }
        running = true;
        receiver = new Thread(this::receiveLoop, "udp-camera-rx-" + bindPort);
        receiver.setDaemon(true);
        receiver.start();
    }

    private void receiveLoop() {
        byte[] buffer = new byte[262144];
        while (running) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                if (running) {
                    continue;
                }
                return;
            }
            try {
                JsonNode node = MAPPER.readTree(packet.getData(), 0, packet.getLength());
                CameraFrameSignal frame = decode(node);
                if (frame != null) {
                    frames.add(frame);
                }
            } catch (IOException ignored) {
                // Drop a malformed frame datagram.
            }
        }
    }

    private CameraFrameSignal decode(JsonNode node) {
        JsonNode pixelsNode = node.get("pixels");
        if (pixelsNode == null || !pixelsNode.isArray()) {
            return null;
        }
        int rows = pixelsNode.size();
        int cols = rows == 0 ? 0 : pixelsNode.get(0).size();
        int[][] pixels = new int[rows][cols];
        for (int y = 0; y < rows; y++) {
            JsonNode row = pixelsNode.get(y);
            for (int x = 0; x < cols && x < row.size(); x++) {
                pixels[y][x] = Math.max(0, Math.min(255, row.get(x).asInt()));
            }
        }
        CameraFrameSignal frame = new CameraFrameSignal();
        frame.setMissionId(node.path("missionId").asText("uav-fpv-live"));
        frame.setUavId(node.path("uavId").asText("uav-1"));
        frame.setTick(node.path("frame").asLong(0));
        frame.setFrameId(node.path("frameId").asText("frame-" + System.nanoTime()));
        frame.setTrackId(node.path("trackId").asText(null));
        frame.setPixels(pixels);
        // Perception / search context for the movement observation assembler.
        frame.setInputName(name);
        copyAttribute(frame, node, "frame");
        copyAttribute(frame, node, "elapsedSeconds");
        copyAttribute(frame, node, "positionOffset");
        copyAttribute(frame, node, "searchBounds");
        copyAttribute(frame, node, "nearestTarget");
        copyAttribute(frame, node, "occludedTarget");
        copyAttribute(frame, node, "photographedTargets");
        copyAttribute(frame, node, "photographedTargetIds");
        copyAttribute(frame, node, "remainingTargets");
        copyAttribute(frame, node, "totalTargets");
        copyAttribute(frame, node, "baseSpeedMetersPerSecond");
        copyAttribute(frame, node, "photoRadiusMeters");
        copyAttribute(frame, node, "headingYawDegrees");
        copyAttribute(frame, node, "obstacleRiskByAction");
        copyAttribute(frame, node, "lidarRiskByAction");
        copyAttribute(frame, node, "lidar");
        copyAttribute(frame, node, "label");
        copyAttribute(frame, node, "detectionBox");
        return frame;
    }

    private static void copyAttribute(CameraFrameSignal frame, JsonNode node, String key) {
        JsonNode value = node.get(key);
        if (value != null && !value.isNull()) {
            frame.attribute(key, MAPPER.convertValue(value, Object.class));
        }
    }

    @Override
    public List<IInputSignal> readSignals() {
        List<IInputSignal> signals = new ArrayList<>();
        CameraFrameSignal frame;
        while ((frame = frames.poll()) != null) {
            signals.add(frame);
        }
        return signals;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public HashMap<String, List<IResultSignal>> getDesiredResults() {
        return new HashMap<>();
    }

    @Override
    public ProcessingFrequency getDefaultProcessingFrequency() {
        return PROCESSING_FREQUENCY;
    }

    public void close() {
        running = false;
        DatagramSocket current = socket;
        if (current != null) {
            current.close();
        }
        if (receiver != null) {
            receiver.interrupt();
        }
    }
}
