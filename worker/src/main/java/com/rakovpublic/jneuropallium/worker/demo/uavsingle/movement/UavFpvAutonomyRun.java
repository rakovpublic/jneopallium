package com.rakovpublic.jneuropallium.worker.demo.uavsingle.movement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.ai.signals.fast.MotorCommandSignal;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import com.rakovpublic.jneuropallium.worker.bridge.mavlink.MavlinkAuditOutput;
import com.rakovpublic.jneuropallium.worker.bridge.mavlink.MavlinkBridgeConfig;
import com.rakovpublic.jneuropallium.worker.bridge.mavlink.MavlinkClientService;
import com.rakovpublic.jneuropallium.worker.bridge.mavlink.MavlinkTelemetryInput;
import com.rakovpublic.jneuropallium.worker.demo.uavsingle.CameraFrameSignal;
import com.rakovpublic.jneuropallium.worker.demo.uavsingle.ClassificationNeuron;
import com.rakovpublic.jneuropallium.worker.demo.uavsingle.ConvolutionalRecognitionNetwork;
import com.rakovpublic.jneuropallium.worker.demo.uavsingle.ConvolutionalRecognitionProcessor;
import com.rakovpublic.jneuropallium.worker.demo.uavsingle.ImageRecognitionNeuron;
import com.rakovpublic.jneuropallium.worker.demo.uavsingle.RecognitionFeedbackSignal;
import com.rakovpublic.jneuropallium.worker.demo.uavsingle.RecognitionLearningNeuron;
import com.rakovpublic.jneuropallium.worker.demo.uavsingle.RecognitionNetworkConfig;
import com.rakovpublic.jneuropallium.worker.demo.uavsingle.RecognitionResultSignal;
import com.rakovpublic.jneuropallium.worker.demo.uavsingle.TargetClassification;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.layers.impl.SimpleResultWrapper;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import io.dronefleet.mavlink.common.GlobalPositionInt;
import io.dronefleet.mavlink.common.MavSeverity;
import io.dronefleet.mavlink.common.Statustext;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Standalone full-loop runner for the autonomous FPV search-and-photograph model.
 *
 * <p>It assembles the architecture the way a separate jneopallium worker process would:
 * <pre>
 *   (synthetic) sim --GLOBAL_POSITION_INT--&gt; MAVLink bridge --&gt; MavlinkTelemetryInput (IInitInput)
 *        FPV frame --&gt; ImageRecognitionNeuron (trained recognition layers) --&gt; photo of target
 *        assembled obs --&gt; MovementObservationInitInput (IInitInput) --&gt; MovementDecisionNeuron
 *            --&gt; MotorCommandSignal --&gt; MovementCommandOutputAggregator (IOutputAggregator)
 *            --&gt; MAVLink STATUSTEXT "JM:vx,vy,alt,yaw" --&gt; (synthetic) sim applies it
 * </pre>
 *
 * <p>The {@link LoopbackMavlinkTransport} stands in for the live UDP/SITL wire so the whole loop is
 * verifiable without CARLA-Air running; the live run swaps the transport + the camera/frame source
 * and is otherwise identical. Artifacts (recognized-target photos, per-tick navigation/command
 * events, the trained movement policy) are written under the output directory as evidence.
 */
public final class UavFpvAutonomyRun {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String MOVE_TAG = "UAV.MOVE";
    private static final double VIEW_RADIUS = 30.0;
    private static final double FRAME_SCALE = 32.0;

    private UavFpvAutonomyRun() {
    }

    private static final class Target {
        final String id;
        final TargetClassification classification;
        final double x;
        final double y;
        boolean photographed;

        Target(String id, TargetClassification classification, double x, double y) {
            this.id = id;
            this.classification = classification;
            this.x = x;
            this.y = y;
        }
    }

    public static void main(String[] args) throws Exception {
        String mode = stringArg(args, "--mode", "selftest");
        Path output = Path.of(stringArg(args, "--output", "target/uav-fpv-autonomy")).toAbsolutePath();
        if ("live".equals(mode)) {
            runLive(args, output);
            return;
        }
        if ("train-recognition".equals(mode)) {
            runTrainRecognition(args, output);
            return;
        }
        if ("eval-recognition".equals(mode)) {
            runEvalRecognition(args, output);
            return;
        }
        if ("train-coverage".equals(mode)) {
            runTrainCoverage(args, output);
            return;
        }
        runSelfTest(intArg(args, "--ticks", 500), output);
    }

    /**
     * Deterministic full-loop verification: a synthetic sim + synthetic FPV frames exercise the
     * exact IInitInput / network / IOutputAggregator / MAVLink boundaries (via the loopback
     * transport) and produce recognized-target photos + the decision/command stream.
     */
    private static void runSelfTest(int ticks, Path output) throws IOException {
        Path photos = output.resolve("photos");
        Files.createDirectories(photos);
        Path events = output.resolve("navigation-events.jsonl");
        Files.writeString(events, "", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        // --- synthetic world: targets to find inside a search area ---
        List<Target> targets = List.of(
                new Target("vehicle-1", TargetClassification.VEHICLE_TO_INSPECT, -90.0, -60.0),
                new Target("personnel-1", TargetClassification.INFANTRY, 70.0, -40.0),
                new Target("vehicle-2", TargetClassification.VEHICLE_TO_INSPECT, 40.0, 95.0),
                new Target("personnel-2", TargetClassification.INFANTRY, -75.0, 80.0));
        double margin = 30.0;
        double minX = targets.stream().mapToDouble(t -> t.x).min().orElse(-100.0) - margin;
        double maxX = targets.stream().mapToDouble(t -> t.x).max().orElse(100.0) + margin;
        double minY = targets.stream().mapToDouble(t -> t.y).min().orElse(-100.0) - margin;
        double maxY = targets.stream().mapToDouble(t -> t.y).max().orElse(100.0) + margin;

        // --- trained recognition layers ---
        RecognitionNetworkConfig recognitionConfig = RecognitionNetworkConfig.fpv1080p();
        ConvolutionalRecognitionNetwork recognitionNetwork = new ConvolutionalRecognitionNetwork(recognitionConfig);
        ImageRecognitionNeuron recognitionNeuron = new ImageRecognitionNeuron(recognitionNetwork);

        // --- movement policy + reinforcement learning ---
        MovementRuntimeConfig movementConfig = MovementRuntimeConfig.carlaAirLive();
        movementConfig.applyOverrides(Map.of("explorationSigma", 0.02));
        MovementPolicyNetwork policy = new MovementPolicyNetwork(movementConfig);
        MovementDecisionNeuron decisionNeuron = new MovementDecisionNeuron(policy);
        MovementLearningNeuron learningNeuron = new MovementLearningNeuron(policy);

        // --- MAVLink bridge: telemetry in, advisory movement command out (sim-only) ---
        LoopbackMavlinkTransport transport = new LoopbackMavlinkTransport();
        MavlinkBridgeConfig bridgeConfig = new MavlinkBridgeConfig(
                List.of(new MavlinkBridgeConfig.ConnectionConfig(
                        "SIM", MavlinkBridgeConfig.Transport.UDP, "127.0.0.1", 14550, null, null, List.of(1))),
                true,
                List.of(new MavlinkBridgeConfig.ReadBindingConfig(
                        "DRONE-POS", "SIM", 1, 1, "GLOBAL_POSITION_INT",
                        MavlinkBridgeConfig.ReadSignalKind.PROPRIOCEPTIVE, "UAV.POS", null, 1)),
                List.of(),
                List.of(new MavlinkBridgeConfig.WriteBindingConfig(
                        "MOVE-CMD", "SIM", 1, 1, "STATUSTEXT", MOVE_TAG,
                        null, movementConfig.getMaxSpeedMetersPerSecond(), null, null)),
                new MavlinkBridgeConfig.AuditConfig(output.resolve("mavlink-audit.jsonl").toString()),
                Map.of("MOVE-CMD", BridgeSafetyMode.ADVISORY),
                null);
        MavlinkAuditOutput audit = new MavlinkAuditOutput(output.resolve("mavlink-audit.jsonl"));
        MavlinkClientService mavlink = new MavlinkClientService(bridgeConfig, Map.of("SIM", transport), audit);
        mavlink.start();

        // --- IInitInput / IOutputAggregator boundaries ---
        MavlinkTelemetryInput telemetryInput = new MavlinkTelemetryInput("uav-telemetry", mavlink, List.of("DRONE-POS"));
        MovementObservationInitInput movementInput = new MovementObservationInitInput("uav-movement-observation");
        MovementCommandOutputAggregator commandAggregator = new MovementCommandOutputAggregator(mavlink, audit);

        // --- synthetic drone pose (NED metres, local to the search area origin) ---
        double droneX = 0.0;
        double droneY = 0.0;
        double altitude = movementConfig.getInitialAltitudeMeters();
        double headingYaw = 0.0;
        double dt = movementConfig.getCommandHoldSeconds();
        double baseSpeed = movementConfig.getMaxSpeedMetersPerSecond();

        int photographed = 0;
        int telemetrySignals = 0;
        int recognitions = 0;

        for (int frame = 1; frame <= ticks; frame++) {
            double elapsed = frame * dt;

            // sim -> MAVLink: publish telemetry; bridge -> IInitInput drains it as ProprioceptiveSignal.
            transport.deliver(1, 1, GlobalPositionInt.builder()
                    .timeBootMs(frame * 100L)
                    .lat((int) Math.round(droneX * 1000))
                    .lon((int) Math.round(droneY * 1000))
                    .alt((int) Math.round(altitude * 1000))
                    .relativeAlt((int) Math.round(altitude * 1000))
                    .vx((int) Math.round(Math.cos(Math.toRadians(headingYaw)) * baseSpeed * 100))
                    .vy((int) Math.round(Math.sin(Math.toRadians(headingYaw)) * baseSpeed * 100))
                    .vz(0)
                    .hdg((int) Math.round(headingYaw * 100))
                    .build());
            List<IInputSignal> telemetry = telemetryInput.readSignals();
            telemetrySignals += telemetry.size();

            // recognition: photograph the nearest unphotographed target that is in view.
            Target nearest = nearestUnphotographed(targets, droneX, droneY);
            Map<String, Object> photoRow = null;
            if (nearest != null) {
                double distance = Math.hypot(nearest.x - droneX, nearest.y - droneY);
                if (distance <= VIEW_RADIUS) {
                    CameraFrameSignal cameraFrame = syntheticFrame(nearest, frame);
                    RecognitionResultSignal result = recognitionNeuron.recognize(cameraFrame);
                    recognitions++;
                    if (result.getClassification() == nearest.classification && result.getConfidence() >= 0.45) {
                        nearest.photographed = true;
                        photographed++;
                        Path photo = photos.resolve(nearest.id + "-frame" + frame + ".png");
                        writePhoto(photo, cameraFrame.getPixels());
                        photoRow = new LinkedHashMap<>();
                        photoRow.put("targetId", nearest.id);
                        photoRow.put("classification", result.getClassification().name());
                        photoRow.put("confidence", round(result.getConfidence()));
                        photoRow.put("distanceMeters", round(distance));
                        photoRow.put("photo", photo.toString());
                    }
                }
            }

            // assemble the public observation and push it through the movement IInitInput boundary.
            MovementObservationSignal observation = buildObservation(
                    frame, elapsed, droneX, droneY, headingYaw, minX, maxX, minY, maxY,
                    targets, photographed, baseSpeed, headingYaw);
            movementInput.offer(observation);

            MovementPolicyNetwork.DecisionOutcome outcome = null;
            for (IInputSignal signal : movementInput.readSignals()) {
                if (signal instanceof MovementObservationSignal obs) {
                    outcome = decisionNeuron.decideOutcome(obs);
                }
            }
            if (outcome == null) {
                continue;
            }

            // movement command -> IOutputAggregator -> MAVLink advisory STATUSTEXT.
            MotorCommandSignal command = outcome.getDecision().getCommand().toSignal();
            command.setName(MOVE_TAG);
            command.setExecute(true);
            IResult wrapped = new SimpleResultWrapper<>(command, 7L);
            commandAggregator.save(List.of(wrapped), System.currentTimeMillis(), frame, null);

            // MAVLink -> sim: apply the advisory command the bridge emitted.
            String egressText = null;
            for (LoopbackMavlinkTransport.Egress egress : transport.drainSent()) {
                if (egress.payload() instanceof Statustext st && st.text() != null && st.text().startsWith("JM:")) {
                    egressText = st.text();
                    double[] cmd = parseMove(st.text());
                    droneX += cmd[0] * dt;
                    droneY += cmd[1] * dt;
                    altitude = cmd[2];
                    headingYaw = cmd[3];
                }
            }

            writeEvent(events, buildEventRow(frame, elapsed, droneX, droneY, altitude, headingYaw,
                    telemetry.size(), outcome, egressText, photographed, targets.size(), photoRow));
        }

        // discrete event reinforcement: penalize the policy for any target it never reached.
        for (Target target : targets) {
            if (!target.photographed) {
                MovementReinforcementSignal penalty = new MovementReinforcementSignal(
                        ticks, -0.6, "MOVEMENT_POLICY_MISSED_TARGET");
                penalty.withExtra("missedTargetId", target.id);
                learningNeuron.applyReward(penalty);
            }
        }

        // emit the trained movement policy (the warm-startable movementPolicy model block).
        Path policyOut = output.resolve("trained-movement-policy.json");
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(policyOut.toFile(), policy.toModelMap());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("mode", "uav-fpv-autonomy-loopback");
        summary.put("ticks", ticks);
        summary.put("targetsPlaced", targets.size());
        summary.put("targetsPhotographed", photographed);
        summary.put("recognitionsRun", recognitions);
        summary.put("telemetrySignalsDrained", telemetrySignals);
        summary.put("movementDecisions", policy.getDecisionsMade());
        summary.put("reinforcementUpdates", policy.getReinforcementUpdates());
        summary.put("coverageCellsVisited", policy.getCoverageCellsVisited());
        summary.put("finalDroneOffset", Map.of("x", round(droneX), "y", round(droneY), "altitude", round(altitude)));
        summary.put("policy", policy.summary());
        summary.put("photosDir", photos.toString());
        summary.put("navigationEvents", events.toString());
        summary.put("trainedMovementPolicy", policyOut.toString());
        Path summaryPath = output.resolve("summary.json");
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(summaryPath.toFile(), summary);

        mavlink.close();
        audit.close();
        System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(summary));
    }

    /**
     * Live run against the CARLA-Air thin bridge over the real UDP MAVLink wire + the camera UDP
     * channel. Telemetry and the movement command flow on MAVLink; FPV frames + perception/search
     * context flow on the camera channel; recognized targets trigger a {@code JCAP} capture advisory
     * the bridge uses to save the full-resolution FPV photo.
     */
    private static void runLive(String[] args, Path output) throws Exception {
        int mavlinkPort = intArg(args, "--mavlink-port", 14550);
        int cameraPort = intArg(args, "--camera-port", 14552);
        double durationSeconds = doubleArg(args, "--duration", 180.0);
        double idleExitSeconds = doubleArg(args, "--idle-exit-seconds", 5.0);
        double photoConfidenceThreshold = doubleArg(args, "--photo-confidence-threshold", 0.70);
        Path photos = output.resolve("photos");
        Files.createDirectories(photos);
        Path events = output.resolve("navigation-events.jsonl");
        Files.writeString(events, "", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        RecognitionNetworkConfig recognitionConfig = RecognitionNetworkConfig.fpv1080p();
        ConvolutionalRecognitionNetwork recognitionNetwork = new ConvolutionalRecognitionNetwork(recognitionConfig);
        ImageRecognitionNeuron recognitionNeuron = new ImageRecognitionNeuron(recognitionNetwork);
        RecognitionLearningNeuron recognitionLearning = new RecognitionLearningNeuron(recognitionNetwork);
        String recognitionModelArg = stringArg(args, "--recognition-model", null);
        Path recognitionModelPath = recognitionModelArg == null ? null : Path.of(recognitionModelArg).toAbsolutePath();
        int restoredPrototypes = loadRecognitionModel(recognitionNeuron, recognitionNetwork, recognitionModelPath);
        System.out.println("[live] recognition prototypes loaded: " + restoredPrototypes
                + (recognitionModelPath == null ? " (no --recognition-model; using template prototypes)" : ""));

        MovementRuntimeConfig movementConfig = MovementRuntimeConfig.carlaAirLive();
        MovementPolicyNetwork policy = new MovementPolicyNetwork(movementConfig);
        String movementModelArg = stringArg(args, "--movement-model", null);
        if (movementModelArg != null && Files.exists(Path.of(movementModelArg))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> trainedMovement = MAPPER.readValue(Files.readString(Path.of(movementModelArg)), Map.class);
            System.out.println("[live] trained movement policy loaded: "
                    + policy.loadWeights(trainedMovement) + " neurons restored");
        }
        MovementDecisionNeuron decisionNeuron = new MovementDecisionNeuron(policy);

        UdpMavlinkTransport transport = new UdpMavlinkTransport(mavlinkPort);
        MavlinkBridgeConfig bridgeConfig = new MavlinkBridgeConfig(
                List.of(new MavlinkBridgeConfig.ConnectionConfig(
                        "SIM", MavlinkBridgeConfig.Transport.UDP, "0.0.0.0", mavlinkPort, null, null, List.of(1))),
                true,
                List.of(new MavlinkBridgeConfig.ReadBindingConfig(
                        "DRONE-POS", "SIM", 1, 1, "GLOBAL_POSITION_INT",
                        MavlinkBridgeConfig.ReadSignalKind.PROPRIOCEPTIVE, "UAV.POS", null, 1)),
                List.of(),
                List.of(
                        new MavlinkBridgeConfig.WriteBindingConfig(
                                "MOVE-CMD", "SIM", 1, 1, "STATUSTEXT", MOVE_TAG,
                                null, movementConfig.getMaxSpeedMetersPerSecond(), null, null),
                        new MavlinkBridgeConfig.WriteBindingConfig(
                                "PHOTO-CMD", "SIM", 1, 1, "STATUSTEXT", "UAV.PHOTO", null, null, null, null)),
                new MavlinkBridgeConfig.AuditConfig(output.resolve("mavlink-audit.jsonl").toString()),
                Map.of("MOVE-CMD", BridgeSafetyMode.ADVISORY, "PHOTO-CMD", BridgeSafetyMode.ADVISORY),
                null);
        MavlinkAuditOutput audit = new MavlinkAuditOutput(output.resolve("mavlink-audit.jsonl"));
        MavlinkClientService mavlink = new MavlinkClientService(bridgeConfig, Map.of("SIM", transport), audit);
        mavlink.start();

        MavlinkTelemetryInput telemetryInput = new MavlinkTelemetryInput("uav-telemetry", mavlink, List.of("DRONE-POS"));
        MovementObservationInitInput movementInput = new MovementObservationInitInput("uav-movement-observation");
        MovementCommandOutputAggregator commandAggregator = new MovementCommandOutputAggregator(mavlink, audit);
        CameraFrameUdpInput cameraInput = new CameraFrameUdpInput("uav-camera", cameraPort);
        cameraInput.start();

        System.out.println("[live] MAVLink UDP :" + mavlinkPort + ", camera UDP :" + cameraPort
                + " — waiting for the CARLA-Air bridge (telemetry establishes the reply route)...");

        int photographed = 0;
        int telemetrySignals = 0;
        int recognitions = 0;
        Set<String> photographedTargetIds = new HashSet<>();
        long frame = 0;
        long deadline = System.currentTimeMillis() + (long) (durationSeconds * 1000);
        long lastCameraFrameAt = 0L;
        boolean sawCameraFrames = false;
        while (System.currentTimeMillis() < deadline) {
            telemetrySignals += telemetryInput.readSignals().size();
            boolean acted = false;
            for (IInputSignal cameraSignal : cameraInput.readSignals()) {
                if (!(cameraSignal instanceof CameraFrameSignal cameraFrame)) {
                    continue;
                }
                frame++;
                acted = true;
                sawCameraFrames = true;
                lastCameraFrameAt = System.currentTimeMillis();
                syncConfirmedPhotographs(photographedTargetIds,
                        cameraFrame.getAttributes().get("photographedTargetIds"));
                photographed = Math.max(photographed, photographedTargetIds.size());
                RecognitionResultSignal result = recognitionNeuron.recognize(cameraFrame);
                recognitions++;
                Map<String, Object> photoRow = null;
                Object supervisedLabel = cameraFrame.getAttributes().get("label");
                TargetClassification expected = supervisedLabel == null ? null : parseClass(supervisedLabel.toString());
                if (expected != null) {
                    RecognitionFeedbackSignal recognitionFeedback = new RecognitionFeedbackSignal();
                    recognitionFeedback.setTargetId(normalizedTargetId(result.getTargetId()));
                    recognitionFeedback.setExpectedClassification(expected);
                    recognitionFeedback.setPredictedClassification(result.getClassification());
                    recognitionFeedback.setReward(expected == result.getClassification() ? 1.0 : -1.0);
                    recognitionFeedback.setLearningRate(recognitionConfig.getLearningRate());
                    recognitionFeedback.setImageFeatures(result.getImageFeatures());
                    recognitionLearning.learn(recognitionFeedback);
                }
                Map<String, Object> recognitionRow = recognitionRow(
                        result, cameraFrame, photographedTargetIds, expected, photoConfidenceThreshold);
                String targetId = normalizedTargetId(result.getTargetId());
                boolean photoGateAccepted = Boolean.TRUE.equals(recognitionRow.get("photoGateAccepted"));
                if (photoGateAccepted) {
                    mavlink.send("PHOTO-CMD", Statustext.builder()
                            .severity(MavSeverity.MAV_SEVERITY_INFO)
                            .text(fit50("JCAP:" + targetId + ":" + result.getClassification().name()
                                    + ":" + round(result.getConfidence())))
                            .build(), System.currentTimeMillis(), frame);
                    Path photo = photos.resolve("recognized-" + targetId + "-frame" + frame + ".png");
                    writePhoto(photo, cameraFrame.getPixels());
                    photoRow = new LinkedHashMap<>();
                    photoRow.put("targetId", targetId);
                    photoRow.put("classification", result.getClassification().name());
                    photoRow.put("confidence", round(result.getConfidence()));
                    photoRow.put("photo", photo.toString());
                }

                Map<String, Object> obsMap = new LinkedHashMap<>(cameraFrame.getAttributes());
                obsMap.put("missionId", cameraFrame.getMissionId());
                obsMap.put("uavId", cameraFrame.getUavId());
                obsMap.putIfAbsent("frame", frame);
                MovementObservationSignal observation = MovementObservationSignal.fromMap(obsMap);
                movementInput.offer(observation);

                for (IInputSignal movementSignal : movementInput.readSignals()) {
                    if (!(movementSignal instanceof MovementObservationSignal obs)) {
                        continue;
                    }
                    MovementPolicyNetwork.DecisionOutcome outcome = decisionNeuron.decideOutcome(obs);
                    MotorCommandSignal command = outcome.getDecision().getCommand().toSignal();
                    command.setName(MOVE_TAG);
                    command.setExecute(true);
                    IResult wrapped = new SimpleResultWrapper<>(command, 7L);
                    commandAggregator.save(List.of(wrapped), System.currentTimeMillis(), frame, null);

                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("type", "AUTONOMY_TICK_LIVE");
                    row.put("frame", frame);
                    row.put("recognition", recognitionRow);
                    row.put("decision", outcome.getDecision().asMap());
                    if (outcome.getAutoReinforcement() != null && outcome.getAutoReinforcement().isApplied()) {
                        row.put("reinforcement", outcome.getAutoReinforcement().asMap());
                    }
                    row.put("photographedTargets", photographed);
                    if (photoRow != null) {
                        row.put("photographed", photoRow);
                    }
                    writeEvent(events, row);
                }
            }
            if (!acted) {
                long now = System.currentTimeMillis();
                if (sawCameraFrames && now - lastCameraFrameAt > (long) (idleExitSeconds * 1000)) {
                    System.out.println("[live] camera stream idle for " + idleExitSeconds
                            + " s; flushing trained model and summary.");
                    break;
                }
                Thread.sleep(20);
            }
        }

        Path policyOut = output.resolve("trained-movement-policy.json");
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(policyOut.toFile(), policy.toModelMap());
        Path recognitionOut = recognitionModelPath == null
                ? output.resolve("recognition-model.json").toAbsolutePath()
                : recognitionModelPath;
        int savedRecognitionPrototypes = saveRecognitionModel(recognitionNetwork, recognitionOut);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("mode", "uav-fpv-autonomy-live");
        summary.put("durationSeconds", durationSeconds);
        summary.put("idleExitSeconds", idleExitSeconds);
        summary.put("cameraFramesProcessed", frame);
        summary.put("recognitionsRun", recognitions);
        summary.put("targetsPhotographed", photographed);
        summary.put("telemetrySignalsDrained", telemetrySignals);
        summary.put("movementDecisions", policy.getDecisionsMade());
        summary.put("reinforcementUpdates", policy.getReinforcementUpdates());
        summary.put("policy", policy.summary());
        summary.put("photosDir", photos.toString());
        summary.put("navigationEvents", events.toString());
        summary.put("trainedMovementPolicy", policyOut.toString());
        summary.put("trainedRecognitionModel", recognitionOut.toString());
        summary.put("classifierPrototypesSaved", savedRecognitionPrototypes);
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(output.resolve("summary.json").toFile(), summary);

        cameraInput.close();
        mavlink.close();
        audit.close();
        System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(summary));
    }

    private static void syncConfirmedPhotographs(Set<String> photographedTargetIds, Object confirmedIds) {
        if (!(confirmedIds instanceof Iterable<?> values)) {
            return;
        }
        photographedTargetIds.clear();
        for (Object value : values) {
            if (value != null) {
                photographedTargetIds.add(value.toString());
            }
        }
    }

    private static String fit50(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 50 ? text.substring(0, 50) : text;
    }

    private static double doubleArg(String[] args, String key, double fallback) {
        String value = stringArg(args, key, null);
        if (value == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    /**
     * Phase 1 — train the recognition classifier prototypes on real (or, for verification,
     * synthetic) labeled FPV crops. The thin bridge's {@code --mode pretrain} flies near each
     * CARLA vehicle/human, captures the FPV crop, and streams it with a {@code label} on the camera
     * channel; here each labeled frame runs through the recognition layers and a
     * {@link RecognitionFeedbackSignal} adjusts that class's prototype toward the observed features.
     * The trained prototypes are written to {@code --recognition-model} for the loop / final runs.
     */
    private static void runTrainRecognition(String[] args, Path output) throws Exception {
        int cameraPort = intArg(args, "--camera-port", 14552);
        double durationSeconds = doubleArg(args, "--duration", 120.0);
        int synthetic = intArg(args, "--synthetic", 0);
        Path modelOut = Path.of(stringArg(args, "--recognition-model",
                output.resolve("recognition-model.json").toString())).toAbsolutePath();
        Files.createDirectories(output);

        RecognitionNetworkConfig config = RecognitionNetworkConfig.fpv1080p();
        config.setLearningRate(doubleArg(args, "--recognition-learning-rate", config.getLearningRate()));
        ConvolutionalRecognitionNetwork network = new ConvolutionalRecognitionNetwork(config);
        ImageRecognitionNeuron recognitionNeuron = new ImageRecognitionNeuron(network);
        RecognitionLearningNeuron recognitionLearning = new RecognitionLearningNeuron(network);
        ensureClassifiers(recognitionNeuron);
        int restoredPrototypes = loadRecognitionModel(recognitionNeuron, network, modelOut);

        Map<String, Integer> trained = new LinkedHashMap<>();
        int frames = 0;
        String datasetArg = stringArg(args, "--dataset", null);
        if (datasetArg != null) {
            Path dataset = Path.of(datasetArg).toAbsolutePath();
            for (String line : Files.readAllLines(dataset, StandardCharsets.UTF_8)) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                JsonNode node = MAPPER.readTree(line);
                CameraFrameSignal frame = datasetFrame(node);
                TargetClassification target = parseClass(node.path("label").asText(""));
                if (target == null) {
                    continue;
                }
                trainOne(recognitionNeuron, recognitionLearning, config, frame, target, trained);
                frames++;
            }
        } else if (synthetic > 0) {
            Random rng = new Random(11);
            TargetClassification[] classes = {TargetClassification.VEHICLE_TO_INSPECT, TargetClassification.INFANTRY};
            for (int i = 0; i < synthetic; i++) {
                for (TargetClassification target : classes) {
                    trainOne(recognitionNeuron, recognitionLearning, config, noisyFrame(target, i, rng), target, trained);
                    frames++;
                }
            }
        } else {
            CameraFrameUdpInput cameraInput = new CameraFrameUdpInput("uav-recognition-train", cameraPort);
            cameraInput.start();
            System.out.println("[train] recognition trainer listening on camera UDP :" + cameraPort
                    + " for labeled FPV crops...");
            long deadline = System.currentTimeMillis() + (long) (durationSeconds * 1000);
            while (System.currentTimeMillis() < deadline) {
                boolean acted = false;
                for (IInputSignal signal : cameraInput.readSignals()) {
                    if (!(signal instanceof CameraFrameSignal frame)) {
                        continue;
                    }
                    Object label = frame.getAttributes().get("label");
                    TargetClassification target = label == null ? null : parseClass(label.toString());
                    if (target == null) {
                        continue;
                    }
                    trainOne(recognitionNeuron, recognitionLearning, config, frame, target, trained);
                    frames++;
                    acted = true;
                }
                if (!acted) {
                    Thread.sleep(20);
                }
            }
            cameraInput.close();
        }

        int saved = saveRecognitionModel(network, modelOut);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("mode", "uav-recognition-training");
        summary.put("labeledFramesTrained", frames);
        summary.put("perClassUpdates", trained);
        summary.put("restoredClassifierPrototypes", restoredPrototypes);
        summary.put("classifierPrototypesSaved", saved);
        summary.put("recognitionModel", modelOut.toString());
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(output.resolve("recognition-training-summary.json").toFile(), summary);
        System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(summary));
    }

    private static void runEvalRecognition(String[] args, Path output) throws Exception {
        Path dataset = Path.of(stringArg(args, "--dataset", "")).toAbsolutePath();
        Path model = Path.of(stringArg(args, "--recognition-model",
                output.resolve("recognition-model.json").toString())).toAbsolutePath();
        double threshold = doubleArg(args, "--photo-confidence-threshold", 0.70);
        Files.createDirectories(output);

        RecognitionNetworkConfig config = RecognitionNetworkConfig.fpv1080p();
        ConvolutionalRecognitionNetwork network = new ConvolutionalRecognitionNetwork(config);
        ImageRecognitionNeuron recognitionNeuron = new ImageRecognitionNeuron(network);
        int restored = loadRecognitionModel(recognitionNeuron, network, model);

        Path predictions = output.resolve("recognition-eval-predictions.jsonl");
        Files.writeString(predictions, "", StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
        Map<String, Map<String, Integer>> confusion = new LinkedHashMap<>();
        Map<String, Integer> perClass = new LinkedHashMap<>();
        int frames = 0;
        int correct = 0;
        int confident = 0;
        int confidentCorrect = 0;
        double confidenceSum = 0.0;
        double minConfidence = Double.POSITIVE_INFINITY;
        double maxConfidence = 0.0;
        for (String line : Files.readAllLines(dataset, StandardCharsets.UTF_8)) {
            if (line == null || line.isBlank()) {
                continue;
            }
            JsonNode node = MAPPER.readTree(line);
            CameraFrameSignal frame = datasetFrame(node);
            TargetClassification expected = parseClass(node.path("label").asText(""));
            if (expected == null) {
                continue;
            }
            RecognitionResultSignal result = recognitionNeuron.recognize(frame);
            frames++;
            boolean classCorrect = expected == result.getClassification();
            boolean accepted = result.getClassification() != TargetClassification.UNKNOWN_OBJECT
                    && result.getConfidence() > threshold;
            if (classCorrect) {
                correct++;
            }
            if (accepted) {
                confident++;
            }
            if (accepted && classCorrect) {
                confidentCorrect++;
            }
            confidenceSum += result.getConfidence();
            minConfidence = Math.min(minConfidence, result.getConfidence());
            maxConfidence = Math.max(maxConfidence, result.getConfidence());
            perClass.merge(expected.name(), 1, Integer::sum);
            confusion.computeIfAbsent(expected.name(), ignored -> new LinkedHashMap<>())
                    .merge(result.getClassification().name(), 1, Integer::sum);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("frameId", frame.getFrameId());
            row.put("trackId", frame.getTrackId());
            row.put("expected", expected.name());
            row.put("predicted", result.getClassification().name());
            row.put("confidence", round(result.getConfidence()));
            row.put("accepted", accepted);
            row.put("correct", classCorrect);
            row.put("detectionBox", frame.getAttributes().get("detectionBox"));
            row.put("evidenceMetrics", frame.getAttributes().get("evidenceMetrics"));
            writeEvent(predictions, row);
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("mode", "uav-recognition-eval");
        summary.put("dataset", dataset.toString());
        summary.put("recognitionModel", model.toString());
        summary.put("restoredClassifierPrototypes", restored);
        summary.put("framesEvaluated", frames);
        summary.put("correct", correct);
        summary.put("confidenceThresholdStrictlyGreaterThan", threshold);
        summary.put("confidentPredictions", confident);
        summary.put("confidentCorrect", confidentCorrect);
        summary.put("accuracy", round(frames == 0 ? 0.0 : correct / (double) frames));
        summary.put("confidentCorrectRate", round(frames == 0 ? 0.0 : confidentCorrect / (double) frames));
        summary.put("avgConfidence", round(frames == 0 ? 0.0 : confidenceSum / frames));
        summary.put("minConfidence", round(frames == 0 ? 0.0 : minConfidence));
        summary.put("maxConfidence", round(maxConfidence));
        summary.put("perClassFrames", perClass);
        summary.put("confusion", confusion);
        summary.put("predictions", predictions.toString());
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(output.resolve("recognition-eval-summary.json").toFile(), summary);
        System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(summary));
    }

    @SuppressWarnings("unchecked")
    private static CameraFrameSignal datasetFrame(JsonNode node) {
        CameraFrameSignal frame = new CameraFrameSignal();
        frame.setMissionId(node.path("missionId").asText("dataset"));
        frame.setUavId(node.path("uavId").asText("uav-1"));
        frame.setTick(node.path("frame").asLong(0L));
        frame.setFrameId(node.path("frameId").asText("dataset-" + frame.getTick()));
        frame.setTrackId(node.path("trackId").asText(null));
        frame.setPixels(pixelsFromNode(node.get("pixels")));
        JsonNode label = node.get("label");
        if (label != null && !label.isNull()) {
            frame.attribute("label", label.asText());
        }
        JsonNode box = node.get("detectionBox");
        if (box != null && !box.isNull()) {
            frame.attribute("detectionBox", MAPPER.convertValue(box, Map.class));
        }
        JsonNode evidence = node.get("evidenceMetrics");
        if (evidence != null && !evidence.isNull()) {
            frame.attribute("evidenceMetrics", MAPPER.convertValue(evidence, Map.class));
        }
        return frame;
    }

    private static int[][] pixelsFromNode(JsonNode pixelsNode) {
        if (pixelsNode == null || !pixelsNode.isArray()) {
            return new int[0][0];
        }
        int rows = pixelsNode.size();
        int cols = rows == 0 || !pixelsNode.get(0).isArray() ? 0 : pixelsNode.get(0).size();
        int[][] pixels = new int[rows][cols];
        for (int y = 0; y < rows; y++) {
            JsonNode row = pixelsNode.get(y);
            for (int x = 0; x < cols && row != null && x < row.size(); x++) {
                pixels[y][x] = Math.max(0, Math.min(255, row.get(x).asInt()));
            }
        }
        return pixels;
    }

    private static void trainOne(ImageRecognitionNeuron recognitionNeuron, RecognitionLearningNeuron recognitionLearning,
                                 RecognitionNetworkConfig config, CameraFrameSignal frame,
                                 TargetClassification label, Map<String, Integer> trained) {
        RecognitionResultSignal result = recognitionNeuron.recognize(frame);
        RecognitionFeedbackSignal feedback = new RecognitionFeedbackSignal();
        feedback.setExpectedClassification(label);
        feedback.setPredictedClassification(result.getClassification());
        feedback.setReward(1.0);
        feedback.setLearningRate(config.getLearningRate());
        feedback.setImageFeatures(result.getImageFeatures());
        recognitionLearning.learn(feedback);
        trained.merge(label.name(), 1, Integer::sum);
    }

    private static void ensureClassifiers(ImageRecognitionNeuron recognitionNeuron) {
        CameraFrameSignal warmup = new CameraFrameSignal();
        warmup.setMissionId("warmup");
        warmup.setUavId("warmup");
        warmup.setFrameId("warmup");
        warmup.setTrackId("warmup");
        warmup.setPixels(ConvolutionalRecognitionProcessor.templateFor(TargetClassification.UNKNOWN_OBJECT));
        recognitionNeuron.recognize(warmup);
    }

    private static CameraFrameSignal noisyFrame(TargetClassification classification, int index, Random rng) {
        int[][] template = ConvolutionalRecognitionProcessor.templateFor(classification);
        int[][] pixels = new int[template.length][template[0].length];
        for (int y = 0; y < template.length; y++) {
            for (int x = 0; x < template[0].length; x++) {
                pixels[y][x] = Math.max(0, Math.min(255, template[y][x] + (int) Math.round(rng.nextGaussian() * 18)));
            }
        }
        CameraFrameSignal frame = new CameraFrameSignal();
        frame.setMissionId("train");
        frame.setUavId("uav-1");
        frame.setTick(index);
        frame.setFrameId("train-" + classification.name() + "-" + index);
        frame.setTrackId(classification.name());
        frame.setPixels(pixels);
        return frame;
    }

    private static Map<String, Object> recognitionRow(RecognitionResultSignal result,
                                                      CameraFrameSignal frame,
                                                      Set<String> photographedTargetIds,
                                                      TargetClassification expectedClassification,
                                                      double photoConfidenceThreshold) {
        Map<String, Object> row = new LinkedHashMap<>();
        String targetId = normalizedTargetId(result.getTargetId());
        boolean hasTarget = targetId != null;
        boolean alreadyPhotographed = hasTarget && photographedTargetIds.contains(targetId);
        boolean confident = result.getClassification() != TargetClassification.UNKNOWN_OBJECT
                && result.getConfidence() > photoConfidenceThreshold;
        boolean classMatchesFeedback = expectedClassification == null
                || expectedClassification == result.getClassification();
        row.put("targetId", targetId);
        if (expectedClassification != null) {
            row.put("expectedClassification", expectedClassification.name());
        }
        row.put("classification", result.getClassification().name());
        row.put("confidence", round(result.getConfidence()));
        row.put("photoConfidenceThreshold", round(photoConfidenceThreshold));
        row.put("frameId", result.getFrameId());
        row.put("hasCandidateTarget", hasTarget);
        row.put("alreadyPhotographed", alreadyPhotographed);
        row.put("classMatchesSimulatorFeedback", classMatchesFeedback);
        row.put("photoGateAccepted", confident && hasTarget && !alreadyPhotographed && classMatchesFeedback);
        if (!confident) {
            row.put("photoGateRejectReason",
                    result.getClassification() == TargetClassification.UNKNOWN_OBJECT
                            ? "UNKNOWN_OBJECT" : "CONFIDENCE_NOT_OVER_THRESHOLD");
        } else if (!hasTarget) {
            row.put("photoGateRejectReason", "NO_VISIBLE_CANDIDATE_TARGET");
        } else if (alreadyPhotographed) {
            row.put("photoGateRejectReason", "TARGET_ALREADY_PHOTOGRAPHED");
        } else if (!classMatchesFeedback) {
            row.put("photoGateRejectReason", "CLASS_MISMATCH_TO_SIMULATOR_FEEDBACK");
        }
        Map<String, Double> features = result.getImageFeatures();
        row.put("bestConvolutionalScore", round(features.getOrDefault("bestConvolutionalScore", 0.0)));
        row.put("runnerUpConvolutionalScore", round(features.getOrDefault("runnerUpConvolutionalScore", 0.0)));
        row.put("classificationMargin", round(features.getOrDefault("classificationMargin", 0.0)));
        Object box = frame.getAttributes().get("detectionBox");
        if (box != null) {
            row.put("detectionBox", box);
        }
        return row;
    }

    private static String normalizedTargetId(String targetId) {
        if (targetId == null) {
            return null;
        }
        String trimmed = targetId.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static TargetClassification parseClass(String label) {
        try {
            return TargetClassification.valueOf(label);
        } catch (IllegalArgumentException ignored) {
            String upper = label.toUpperCase(Locale.ROOT);
            if (upper.contains("HUMAN") || upper.contains("PERSON") || upper.contains("INFANTRY") || upper.contains("WALKER")) {
                return TargetClassification.INFANTRY;
            }
            if (upper.contains("VEHICLE") || upper.contains("CAR") || upper.contains("TRUCK")) {
                return TargetClassification.VEHICLE_TO_INSPECT;
            }
            return null;
        }
    }

    private static int saveRecognitionModel(ConvolutionalRecognitionNetwork network, Path path) throws IOException {
        Map<String, Object> model = new LinkedHashMap<>();
        Map<String, Object> prototypes = new LinkedHashMap<>();
        Map<String, Object> exemplars = new LinkedHashMap<>();
        for (Map.Entry<TargetClassification, ClassificationNeuron> entry : network.getClassifierNeurons().entrySet()) {
            prototypes.put(entry.getKey().name(), entry.getValue().getPrototype());
            exemplars.put(entry.getKey().name(), entry.getValue().getExemplars());
        }
        model.put("classifierPrototypes", prototypes);
        model.put("classifierExemplars", exemplars);
        model.put("classes", new java.util.ArrayList<>(prototypes.keySet()));
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), model);
        return prototypes.size();
    }

    private static int loadRecognitionModel(ImageRecognitionNeuron recognitionNeuron,
                                            ConvolutionalRecognitionNetwork network, Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            return 0;
        }
        ensureClassifiers(recognitionNeuron);
        JsonNode prototypes = MAPPER.readTree(path.toFile()).get("classifierPrototypes");
        if (prototypes == null) {
            return 0;
        }
        int loaded = 0;
        var fields = prototypes.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            TargetClassification target = parseClass(entry.getKey());
            ClassificationNeuron neuron = target == null ? null : network.getClassifierNeurons().get(target);
            if (neuron == null) {
                continue;
            }
            Map<String, Double> prototype = new LinkedHashMap<>();
            var values = entry.getValue().fields();
            while (values.hasNext()) {
                Map.Entry<String, JsonNode> value = values.next();
                prototype.put(value.getKey(), value.getValue().asDouble());
            }
            neuron.setPrototype(prototype);
            loaded++;
        }
        JsonNode exemplarSets = MAPPER.readTree(path.toFile()).get("classifierExemplars");
        if (exemplarSets != null) {
            var exemplarFields = exemplarSets.fields();
            while (exemplarFields.hasNext()) {
                Map.Entry<String, JsonNode> entry = exemplarFields.next();
                TargetClassification target = parseClass(entry.getKey());
                ClassificationNeuron neuron = target == null ? null : network.getClassifierNeurons().get(target);
                if (neuron == null || !entry.getValue().isArray()) {
                    continue;
                }
                List<Map<String, Double>> loadedExemplars = new ArrayList<>();
                for (JsonNode exemplarNode : entry.getValue()) {
                    Map<String, Double> exemplar = new LinkedHashMap<>();
                    var values = exemplarNode.fields();
                    while (values.hasNext()) {
                        Map.Entry<String, JsonNode> value = values.next();
                        exemplar.put(value.getKey(), value.getValue().asDouble());
                    }
                    loadedExemplars.add(exemplar);
                }
                neuron.setExemplars(loadedExemplars);
            }
        }
        return loaded;
    }

    /**
     * Episodic reinforcement-learning trainer for area coverage. Each episode flies the policy over
     * a fast kinematic model from a (rotating) corner; the camera footprint marks covered cells; the
     * reward is new-cells-covered minus a per-step time cost (so the policy is pushed toward covering
     * the whole specified area in as few steps as possible). Training exits when the policy
     * <em>sustains</em> full camera coverage in near-optimal time — i.e. the last {@code window}
     * episodes all reach 100% coverage and the worst of them is within {@code tolerance}x the best
     * steps-to-cover seen (a near-optimal-time plateau). The trained policy is written for the live
     * run to load.
     */
    private static void runTrainCoverage(String[] args, Path output) throws IOException {
        double half = doubleArg(args, "--area-meters", 150.0);
        double footprint = doubleArg(args, "--camera-footprint", 45.0);
        int maxEpisodes = intArg(args, "--episodes", 800);
        int maxSteps = intArg(args, "--max-steps", 320);
        double coverageGrid = doubleArg(args, "--coverage-grid", 35.0);
        double maxSpeed = doubleArg(args, "--max-speed", 24.0);
        int lidarTrainingObstacles = intArg(args, "--lidar-training-obstacles", 6);
        double learningRate = doubleArg(args, "--learning-rate", 0.01);
        String initialModelArg = stringArg(args, "--initial-movement-model", null);
        double minExploration = doubleArg(args, "--min-exploration", explorationDefault(args));
        double deterministicSeed = doubleArg(args, "--deterministic-seed", 17.0);
        Path modelOut = Path.of(stringArg(args, "--movement-model",
                output.resolve("trained-movement-policy.json").toString())).toAbsolutePath();
        Files.createDirectories(output);
        Path log = output.resolve("coverage-training.jsonl");
        Files.writeString(log, "", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        MovementRuntimeConfig config = MovementRuntimeConfig.carlaAirLive();
        config.applyOverrides(Map.of(
                "learningRate", learningRate,
                "coverageGridSizeMeters", coverageGrid,
                "maxSpeedMetersPerSecond", maxSpeed,
                "minSpeedMetersPerSecond", Math.min(8.0, maxSpeed),
                "maxAltitudeMeters", Math.max(110.0, footprint + 10.0),
                "initialAltitudeMeters", Math.max(70.0, footprint),
                "deterministicSeed", deterministicSeed));
        MovementPolicyNetwork policy = new MovementPolicyNetwork(config);
        policy.setAutoReinforce(false);
        double baseline = 0.0;
        double baselineAlpha = 0.02;
        double explorationStart = doubleArg(args, "--exploration", 0.2);
        // Warm-start the policy toward nearest-frontier coverage (a near-optimal sweep strategy):
        // strong pull to the nearest uncovered cell + unvisited look-ahead + a preference for moving
        // fast over changing altitude/hovering. RL then fine-tunes these weights for time-optimality.
        for (MovementActionNeuron neuron : policy.getNeurons()) {
            Map<String, Double> weights = neuron.getWeights();
            weights.put("target_view_alignment", 0.35);
            weights.put("coverage_unvisited", 1.5);
            weights.put("coverage_frontier_alignment", 4.2);
            weights.put("sweep_alignment", 3.2);
            weights.put("movement_energy", 1.2);
            weights.put("stuck_escape", 0.5);
            weights.put("obstacle_clearance", 2.8);
            weights.put("lidar_corridor_clearance", 3.4);
            weights.put("lidar_escape_route", 3.0);
            weights.put("altitude_window", 0.0);
            weights.put("occlusion_escape", 0.0);
            weights.put("search_progress", 0.3);
        }
        int restoredInitialPolicy = 0;
        if (initialModelArg != null && Files.exists(Path.of(initialModelArg))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> initialModel = MAPPER.readValue(Files.readString(Path.of(initialModelArg)), Map.class);
            restoredInitialPolicy = policy.loadWeights(initialModel);
            config.applyOverrides(Map.of("learningRate", learningRate));
        }

        double minX = -half;
        double maxX = half;
        double minY = -half;
        double maxY = half;
        double grid = config.getCoverageGridSizeMeters();
        int cols = (int) Math.ceil((maxX - minX) / grid);
        int rows = (int) Math.ceil((maxY - minY) / grid);
        int totalCells = cols * rows;
        double dt = config.getCommandHoldSeconds();
        double baseSpeed = config.getMaxSpeedMetersPerSecond();
        double coverageWeight = 0.6;
        double stepCost = 0.04;
        int window = 10;
        double tolerance = 1.40;   // near-optimal band; corner-to-corner sweep length varies

        double[][] corners = {
                {minX + grid * 0.5, minY + grid * 0.5}, {maxX - grid * 0.5, minY + grid * 0.5},
                {maxX - grid * 0.5, maxY - grid * 0.5}, {minX + grid * 0.5, maxY - grid * 0.5}};
        java.util.ArrayDeque<Integer> recent = new java.util.ArrayDeque<>();
        int bestSteps = Integer.MAX_VALUE;
        int convergedEpisode = -1;
        int fullCoverageEpisodes = 0;
        int episodesRun = 0;
        Map<String, Object> bestModelMap = null;
        Map<String, Object> convergedModelMap = null;
        double[][] trainingObstacles = syntheticObstacleRectangles(half, lidarTrainingObstacles);

        for (int episode = 1; episode <= maxEpisodes; episode++) {
            episodesRun = episode;
            policy.resetEpisode();
            // anneal exploration: explore trajectories early, exploit the learned sweep later.
            config.setExplorationSigma(Math.max(minExploration, explorationStart * Math.exp(-episode / 320.0)));
            double[] start = corners[(episode - 1) % corners.length];
            double droneX = start[0];
            double droneY = start[1];
            double yaw = 0.0;
            int coveredPrev = 0;
            int stepsToCover = maxSteps;
            boolean full = false;
            for (int step = 1; step <= maxSteps; step++) {
                MovementObservationSignal obs = coverageObservation(step, droneX, droneY, yaw,
                        minX, maxX, minY, maxY, baseSpeed, footprint);
                applySyntheticLidarRisks(policy, obs, trainingObstacles,
                        Math.max(config.getObstacleRayMeters(), baseSpeed * config.getCommandHoldSeconds() * 3.0));
                policy.markCameraFootprint(droneX, droneY, footprint, obs);
                double[] frontier = policy.nearestUncoveredCell(droneX, droneY, obs);
                if (frontier != null) {
                    obs.setHasNearestTarget(true);
                    obs.setNearestVectorX(frontier[0]);
                    obs.setNearestVectorY(frontier[1]);
                    obs.setNearestDistanceMeters(frontier[2]);
                }
                MovementPolicyNetwork.DecisionOutcome outcome = policy.decide(obs);
                MotorCommand command = outcome.getDecision().getCommand();
                double selectedRisk = obs.lidarRisk(outcome.getDecision().getActionId());
                droneX += command.getVelocityX() * dt;
                droneY += command.getVelocityY() * dt;
                yaw = command.getYawDegrees();
                int covered = policy.markCameraFootprint(droneX, droneY, footprint, obs);
                int newCells = covered - coveredPrev;
                coveredPrev = covered;
                boolean inside = policy.insideBounds(droneX, droneY, obs);
                double reward = coverageWeight * newCells - stepCost + (inside ? 0.0 : -0.6)
                        - selectedRisk * 1.8;
                if (selectedRisk > 0.55) {
                    reward -= 1.4;
                }
                if (newCells == 0 && frontier != null) {
                    reward -= 0.5;   // idling while area is still uncovered is strongly discouraged
                }
                double advantage = reward - baseline;          // REINFORCE with a moving-average baseline
                baseline += baselineAlpha * (reward - baseline);
                Map<String, Object> extras = new LinkedHashMap<>();
                extras.put("newCells", newCells);
                extras.put("coveredFraction", round((double) covered / totalCells));
                extras.put("selectedLidarRisk", round(selectedRisk));
                policy.reinforceEvent(advantage, step, "COVERAGE_TRAINING", extras);
                if (covered >= totalCells) {
                    full = true;
                    stepsToCover = step;
                    break;
                }
            }
            double fraction = (double) policy.getCoverageCellsVisited() / totalCells;
            if (full) {
                fullCoverageEpisodes++;
                if (stepsToCover < bestSteps) {
                    bestSteps = stepsToCover;
                    bestModelMap = policy.toModelMap();   // keep the best (fewest-step) full-coverage policy
                }
            }
            recent.addLast(full ? stepsToCover : maxSteps);
            while (recent.size() > window) {
                recent.removeFirst();
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("episode", episode);
            row.put("coveredFraction", round(fraction));
            row.put("fullCoverage", full);
            row.put("stepsToCover", full ? stepsToCover : null);
            row.put("bestSteps", bestSteps == Integer.MAX_VALUE ? null : bestSteps);
            writeEvent(log, row);
            if (episode % 25 == 0 || (full && episode % 5 == 0)) {
                System.out.printf(Locale.ROOT, "[coverage] ep %d: covered %.0f%% %s bestSteps=%s%n",
                        episode, fraction * 100.0, full ? ("in " + stepsToCover + " steps") : "(partial)",
                        bestSteps == Integer.MAX_VALUE ? "-" : String.valueOf(bestSteps));
            }
            if (recent.size() == window && bestSteps != Integer.MAX_VALUE) {
                boolean allFull = recent.stream().allMatch(s -> s < maxSteps);
                int worst = recent.stream().max(Integer::compareTo).orElse(maxSteps);
                if (allFull && worst <= bestSteps * tolerance) {
                    convergedEpisode = episode;
                    convergedModelMap = policy.toModelMap();
                    break;
                }
            }
        }

        MAPPER.writerWithDefaultPrettyPrinter().writeValue(modelOut.toFile(),
                convergedModelMap != null ? convergedModelMap
                        : (bestModelMap != null ? bestModelMap : policy.toModelMap()));
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("mode", "uav-movement-coverage-training");
        summary.put("areaMeters", (2 * half) + " x " + (2 * half));
        summary.put("coverageGridMeters", grid);
        summary.put("cameraFootprintMeters", footprint);
        summary.put("coverageCellsTotal", totalCells);
        summary.put("lidarTrainingObstacles", trainingObstacles.length);
        summary.put("learningRate", learningRate);
        summary.put("explorationStart", explorationStart);
        summary.put("minExploration", minExploration);
        summary.put("deterministicSeed", (long) deterministicSeed);
        summary.put("initialMovementPolicy", initialModelArg == null ? null : Path.of(initialModelArg).toAbsolutePath().toString());
        summary.put("initialMovementPolicyNeuronsRestored", restoredInitialPolicy);
        summary.put("episodesRun", episodesRun);
        summary.put("fullCoverageEpisodes", fullCoverageEpisodes);
        summary.put("bestStepsToFullCoverage", bestSteps == Integer.MAX_VALUE ? null : bestSteps);
        summary.put("nearOptimalStepLowerBound", theoreticalCoverageLowerBound(half, footprint, grid, baseSpeed,
                config.getCommandHoldSeconds()));
        summary.put("converged", convergedEpisode > 0);
        summary.put("convergedAtEpisode", convergedEpisode > 0 ? convergedEpisode : null);
        summary.put("exitCondition", "sustained full camera coverage over " + window
                + " episodes within " + tolerance + "x best steps-to-cover");
        summary.put("trainedMovementPolicy", modelOut.toString());
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(output.resolve("coverage-training-summary.json").toFile(), summary);
        System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(summary));
    }

    private static double explorationDefault(String[] args) {
        double requested = doubleArg(args, "--exploration", 0.2);
        return requested <= 0.0 ? 0.0 : 0.06;
    }

    private static double[][] syntheticObstacleRectangles(double half, int count) {
        if (count <= 0) {
            return new double[0][0];
        }
        double h = Math.max(120.0, half);
        double[][] base = {
                {-0.58 * h, -0.18 * h, -0.40 * h, 0.52 * h},
                {-0.18 * h, -0.66 * h, 0.02 * h, -0.08 * h},
                {0.22 * h, -0.46 * h, 0.44 * h, 0.18 * h},
                {-0.02 * h, 0.26 * h, 0.50 * h, 0.48 * h},
                {-0.48 * h, -0.56 * h, -0.25 * h, -0.32 * h},
                {0.56 * h, -0.04 * h, 0.72 * h, 0.60 * h},
        };
        return java.util.Arrays.copyOf(base, Math.min(count, base.length));
    }

    private static void applySyntheticLidarRisks(MovementPolicyNetwork policy, MovementObservationSignal obs,
                                                 double[][] rectangles, double lookahead) {
        if (rectangles.length == 0) {
            return;
        }
        obs.setLidarAvailable(true);
        obs.setLidarPoints(rectangles.length * 24);
        double minDistance = Double.POSITIVE_INFINITY;
        for (MovementActionNeuron neuron : policy.getNeurons()) {
            MovementAction action = neuron.getAction();
            double risk = syntheticCorridorRisk(policy, obs, action, rectangles, lookahead);
            obs.getObstacleRiskByAction().put(action.getActionId(), risk);
            obs.getLidarRiskByAction().put(action.getActionId(), risk);
            if (risk > 0.0) {
                minDistance = Math.min(minDistance, Math.max(1.0, lookahead * (1.0 - risk)));
            }
        }
        obs.setLidarMinDistanceMeters(minDistance);
    }

    private static double syntheticCorridorRisk(MovementPolicyNetwork policy, MovementObservationSignal obs,
                                                MovementAction action, double[][] rectangles, double lookahead) {
        double[] vector = policy.actionVector(obs, action);
        if (vector[0] == 0.0 && vector[1] == 0.0) {
            return 0.0;
        }
        double corridorRadius = 8.0;
        double risk = 0.0;
        int samples = 16;
        for (int i = 1; i <= samples; i++) {
            double fraction = i / (double) samples;
            double px = obs.getPositionX() + vector[0] * lookahead * fraction;
            double py = obs.getPositionY() + vector[1] * lookahead * fraction;
            for (double[] rect : rectangles) {
                double distance = distanceToRectangle(px, py, rect);
                if (distance <= corridorRadius) {
                    risk = Math.max(risk, 1.0 - fraction * 0.72);
                }
            }
        }
        return round(Math.max(0.0, Math.min(1.0, risk)));
    }

    private static double distanceToRectangle(double x, double y, double[] rect) {
        double dx = Math.max(Math.max(rect[0] - x, 0.0), x - rect[2]);
        double dy = Math.max(Math.max(rect[1] - y, 0.0), y - rect[3]);
        return Math.hypot(dx, dy);
    }

    private static int theoreticalCoverageLowerBound(double half, double footprint, double grid,
                                                     double speed, double holdSeconds) {
        double area = Math.max(1.0, (2.0 * half) * (2.0 * half));
        double footprintArea = Math.PI * footprint * footprint;
        int areaBound = (int) Math.ceil(area / Math.max(1.0, footprintArea));
        double swath = Math.max(grid, footprint * 1.72);
        int lanes = Math.max(1, (int) Math.ceil((2.0 * half) / swath));
        double pathLength = Math.max(0.0, lanes * (2.0 * half) + Math.max(0, lanes - 1) * swath);
        int pathBound = (int) Math.ceil(pathLength / Math.max(1.0, speed * holdSeconds));
        return Math.max(areaBound, pathBound);
    }

    private static MovementObservationSignal coverageObservation(int frame, double x, double y, double yaw,
                                                                 double minX, double maxX, double minY, double maxY,
                                                                 double baseSpeed, double footprint) {
        MovementObservationSignal obs = new MovementObservationSignal();
        obs.setMissionId("coverage-train");
        obs.setUavId("uav-1");
        obs.setFrame(frame);
        obs.setTick(frame);
        obs.setPositionX(x);
        obs.setPositionY(y);
        obs.setMinX(minX);
        obs.setMaxX(maxX);
        obs.setMinY(minY);
        obs.setMaxY(maxY);
        obs.setHeadingYawDegrees(yaw);
        obs.setBaseSpeedMetersPerSecond(baseSpeed);
        obs.setPhotoRadiusMeters(1.0);
        obs.setCameraFootprintMeters(footprint);
        obs.setTotalTargets(1);
        obs.setRemainingTargets(0);
        obs.setPhotographedTargets(0);
        return obs;
    }

    private static MovementObservationSignal buildObservation(
            int frame, double elapsed, double droneX, double droneY, double headingYaw,
            double minX, double maxX, double minY, double maxY,
            List<Target> targets, int photographed, double baseSpeed, double yaw) {
        MovementObservationSignal obs = new MovementObservationSignal();
        obs.setMissionId("uav-fpv-autonomy");
        obs.setUavId("uav-1");
        obs.setFrame(frame);
        obs.setTick(frame);
        obs.setElapsedSeconds(elapsed);
        obs.setPositionX(droneX);
        obs.setPositionY(droneY);
        obs.setMinX(minX);
        obs.setMaxX(maxX);
        obs.setMinY(minY);
        obs.setMaxY(maxY);
        obs.setHeadingYawDegrees(yaw);
        obs.setBaseSpeedMetersPerSecond(baseSpeed);
        obs.setPhotoRadiusMeters(VIEW_RADIUS);
        int total = targets.size();
        int remaining = (int) targets.stream().filter(t -> !t.photographed).count();
        obs.setTotalTargets(total);
        obs.setRemainingTargets(remaining);
        obs.setPhotographedTargets(photographed);
        Target nearest = nearestUnphotographed(targets, droneX, droneY);
        if (nearest != null) {
            obs.setHasNearestTarget(true);
            obs.setNearestVectorX(nearest.x - droneX);
            obs.setNearestVectorY(nearest.y - droneY);
            obs.setNearestDistanceMeters(Math.hypot(nearest.x - droneX, nearest.y - droneY));
        }
        return obs;
    }

    private static Map<String, Object> buildEventRow(
            int frame, double elapsed, double droneX, double droneY, double altitude, double yaw,
            int telemetryCount, MovementPolicyNetwork.DecisionOutcome outcome, String egressText,
            int photographed, int totalTargets, Map<String, Object> photoRow) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("type", "AUTONOMY_TICK");
        row.put("frame", frame);
        row.put("elapsedSeconds", round(elapsed));
        row.put("droneOffset", Map.of("x", round(droneX), "y", round(droneY), "altitude", round(altitude), "yaw", round(yaw)));
        row.put("telemetrySignalsDrained", telemetryCount);
        row.put("decision", outcome.getDecision().asMap());
        if (outcome.getAutoReinforcement() != null && outcome.getAutoReinforcement().isApplied()) {
            row.put("reinforcement", outcome.getAutoReinforcement().asMap());
        }
        row.put("mavlinkCommand", egressText);
        row.put("photographedTargets", photographed);
        row.put("remainingTargets", totalTargets - photographed);
        if (photoRow != null) {
            row.put("photographed", photoRow);
        }
        return row;
    }

    private static Target nearestUnphotographed(List<Target> targets, double x, double y) {
        Target nearest = null;
        double best = Double.MAX_VALUE;
        for (Target target : targets) {
            if (target.photographed) {
                continue;
            }
            double distance = Math.hypot(target.x - x, target.y - y);
            if (distance < best) {
                best = distance;
                nearest = target;
            }
        }
        return nearest;
    }

    private static CameraFrameSignal syntheticFrame(Target target, int frame) {
        CameraFrameSignal cameraFrame = new CameraFrameSignal();
        cameraFrame.setMissionId("uav-fpv-autonomy");
        cameraFrame.setUavId("uav-1");
        cameraFrame.setTick(frame);
        cameraFrame.setFrameId("frame-" + frame + "-" + target.id);
        cameraFrame.setTrackId(target.id);
        cameraFrame.setPixels(ConvolutionalRecognitionProcessor.templateFor(target.classification));
        return cameraFrame;
    }

    private static void writePhoto(Path path, int[][] pixels) throws IOException {
        int rows = pixels.length;
        int cols = rows == 0 ? 0 : pixels[0].length;
        if (rows == 0 || cols == 0) {
            return;
        }
        int scale = (int) Math.max(1, FRAME_SCALE);
        BufferedImage image = new BufferedImage(cols * scale, rows * scale, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                int value = Math.max(0, Math.min(255, pixels[y][x]));
                int rgb = (value << 16) | (value << 8) | value;
                for (int dy = 0; dy < scale; dy++) {
                    for (int dx = 0; dx < scale; dx++) {
                        image.setRGB(x * scale + dx, y * scale + dy, rgb);
                    }
                }
            }
        }
        ImageIO.write(image, "png", path.toFile());
    }

    private static double[] parseMove(String text) {
        String[] parts = text.substring(3).split(",");
        double[] cmd = new double[4];
        for (int i = 0; i < 4 && i < parts.length; i++) {
            try {
                cmd[i] = Double.parseDouble(parts[i].trim());
            } catch (NumberFormatException ignored) {
                cmd[i] = 0.0;
            }
        }
        return cmd;
    }

    private static void writeEvent(Path events, Map<String, Object> row) throws IOException {
        Files.writeString(events, MAPPER.writeValueAsString(row) + System.lineSeparator(),
                StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private static int intArg(String[] args, String key, int fallback) {
        String value = stringArg(args, key, null);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String stringArg(String[] args, String key, String fallback) {
        for (int i = 0; i < args.length - 1; i++) {
            if (key.equals(args[i])) {
                return args[i + 1];
            }
        }
        return fallback;
    }
}
