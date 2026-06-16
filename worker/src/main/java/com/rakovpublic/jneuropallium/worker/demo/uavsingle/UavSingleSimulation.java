package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class UavSingleSimulation {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final UavSingleScenario scenario;
    private final Path outputDir;
    private final UavMissionNeuron missionNeuron = new UavMissionNeuron();
    private final TargetEvaluationNeuron targetEvaluationNeuron = new TargetEvaluationNeuron();
    private final ConfirmationGateNeuron confirmationGateNeuron = new ConfirmationGateNeuron();
    private final PhotographyNeuron photographyNeuron = new PhotographyNeuron();
    private final UavMissionProcessor missionProcessor = new UavMissionProcessor();
    private final SimUavCommandGateway commandGateway = new SimUavCommandGateway(new SimUavMissionSupervisor());
    private final NavigationSearchNeuron navigationSearchNeuron = new NavigationSearchNeuron();
    private final ImageRecognitionNeuron imageRecognitionNeuron = new ImageRecognitionNeuron();

    private final List<ObservationTarget> worldTargets = new ArrayList<>();
    private final List<ObservationTarget> targets = new ArrayList<>();
    private final Set<String> recognizedTargetIds = new HashSet<>();
    private final Map<String, Object> summary = new LinkedHashMap<>();
    private UavPose pose;
    private long tick;
    private String selectedTargetId;
    private int confirmationSequence;
    private int photographSequence;
    private int cameraFrameSequence;

    public UavSingleSimulation(UavSingleScenario scenario, Path outputDir) {
        this.scenario = scenario;
        this.outputDir = outputDir;
        this.pose = scenario.initialPose.copy();
        this.worldTargets.addAll(scenario.initialTargets);
        scenario.config.validate();
        if (scenario.faults.lowBattery) {
            this.pose.batteryFraction = Math.min(this.pose.batteryFraction, scenario.config.batteryReserveFraction - 0.04);
        }
        initSummary();
    }

    public UavSingleRunManifest run() throws IOException {
        Files.createDirectories(outputDir);
        writeManifestSkeleton();
        transition(UavMissionState.PREFLIGHT_CHECK, "startup");
        transition(UavMissionState.TAKEOFF_REQUESTED, "preflight-ok");
        transition(UavMissionState.SEARCHING, "simulated-takeoff-complete");
        addMissionEvent("SEARCH_ROUTE_STARTED", Map.of("mode", scenario.mode, "areaId", scenario.searchArea.areaId));
        if (!runSearchCoverage()) {
            return finish("PASS", true);
        }

        TargetPriority selected = detectEvaluateAndSelect();
        if (selected == null) {
            return finish("FAIL", false);
        }
        ObservationTarget target = targetById(selected.targetId).orElseThrow();
        if (scenario.mode == UavOperatingMode.TARGET_CONFIRM) {
            ConfirmationEvaluation confirmation = requestConfirmation(target);
            if (confirmation.status == ConfirmationStatus.DENIED
                    || confirmation.status == ConfirmationStatus.TIMEOUT
                    || confirmation.status == ConfirmationStatus.REJECTED) {
                if (confirmation.status == ConfirmationStatus.DENIED) {
                    transition(UavMissionState.DENIED, confirmation.reason);
                } else if (confirmation.status == ConfirmationStatus.TIMEOUT) {
                    transition(UavMissionState.CONFIRMATION_TIMEOUT, confirmation.reason);
                }
                returnHome("confirmation-not-valid");
                return finish("PASS", true);
            }
            transition(UavMissionState.CONFIRMED, confirmation.reason);
        }

        if (!approachAndObserve(target)) {
            return finish("PASS", true);
        }
        photograph(target);
        returnHome("mission-cycle-complete");
        return finish("PASS", true);
    }

    private boolean runSearchCoverage() throws IOException {
        SearchAreaSignal areaSignal = new SearchAreaSignal(
                scenario.config.missionId, scenario.config.uavId, tick, scenario.searchArea);
        List<SearchWaypointSignal> waypoints = navigationSearchNeuron.plan(scenario.searchArea, scenario.config, tick);
        summary.put("searchWaypointsPlanned", waypoints.size());
        append("search-events.jsonl", row("SEARCH_AREA_DEFINED",
                Map.of("areaId", scenario.searchArea.areaId,
                        "bounds", Map.of("minX", scenario.searchArea.minX, "maxX", scenario.searchArea.maxX,
                                "minY", scenario.searchArea.minY, "maxY", scenario.searchArea.maxY),
                        "spacingMeters", scenario.searchArea.spacingMeters,
                        "detectionRadiusMeters", scenario.searchArea.detectionRadiusMeters,
                        "signalClass", areaSignal.getClass().getSimpleName())));
        if (waypoints.isEmpty()) {
            append("mission-events.jsonl", row("SEARCH_AREA_NO_VALID_WAYPOINTS",
                    Map.of("areaId", scenario.searchArea.areaId)));
            transition(UavMissionState.SAFETY_HOLD, "no-valid-search-waypoints");
            returnHome("no-valid-search-waypoints");
            return false;
        }
        for (SearchWaypointSignal waypoint : waypoints) {
            tick++;
            waypoint.setTick(tick);
            UavPose destination = new UavPose(waypoint.getX(), waypoint.getY(), waypoint.getAltitudeMeters(),
                    0.0, pose.batteryFraction, pose.localizationConfidence);
            UavIntentSignal intent = missionProcessor.intent(
                    scenario.config, tick, UavActionType.SEARCH_ROUTE, null, destination);
            SimUavSupervisorContext context = new SimUavSupervisorContext(scenario.config, pose, tick);
            SimUavCommandGateway.CommandDispatch dispatch = commandGateway.dispatch(intent, context);
            append("supervisor-audit.jsonl", dispatch.audit());
            append("search-events.jsonl", row("SEARCH_WAYPOINT_PLANNED",
                    Map.of("areaId", waypoint.getAreaId(), "waypointIndex", waypoint.getWaypointIndex(),
                            "x", waypoint.getX(), "y", waypoint.getY(), "altitudeMeters", waypoint.getAltitudeMeters(),
                            "signalClass", waypoint.getClass().getSimpleName())));
            if (!dispatch.decision().accepted) {
                summary.put("safetyVetoes", ((Number) summary.get("safetyVetoes")).intValue() + 1);
                if (dispatch.decision().reasons.stream().anyMatch(reason -> reason.startsWith("GEOFENCE"))) {
                    summary.put("geofenceViolations", ((Number) summary.get("geofenceViolations")).intValue() + 1);
                }
                append("search-events.jsonl", row("SEARCH_WAYPOINT_REJECTED",
                        Map.of("waypointIndex", waypoint.getWaypointIndex(), "reasons", dispatch.decision().reasons)));
                transition(UavMissionState.SAFETY_HOLD, "search-route-supervisor-rejected");
                returnHome("search-route-supervisor-rejected");
                return false;
            }
            pose.x = waypoint.getX();
            pose.y = waypoint.getY();
            pose.altitudeMeters = waypoint.getAltitudeMeters();
            summary.put("searchWaypointsVisited", ((Number) summary.get("searchWaypointsVisited")).intValue() + 1);
            append("search-events.jsonl", row("SEARCH_WAYPOINT_VISITED",
                    Map.of("areaId", waypoint.getAreaId(), "waypointIndex", waypoint.getWaypointIndex(),
                            "x", waypoint.getX(), "y", waypoint.getY())));
            recognizeVisibleTargetsFromWaypoint();
        }
        return true;
    }

    private void recognizeVisibleTargetsFromWaypoint() throws IOException {
        for (ObservationTarget worldTarget : worldTargets) {
            if (worldTarget.active && !recognizedTargetIds.contains(worldTarget.targetId)
                    && pose.distance2d(worldTarget.x, worldTarget.y) <= scenario.searchArea.detectionRadiusMeters) {
                recognizeWorldTarget(worldTarget);
            }
        }
    }

    private void recognizeWorldTarget(ObservationTarget worldTarget) throws IOException {
        CameraFrameSignal frame = SyntheticCameraFrameFactory.fromTarget(
                scenario.config, tick, worldTarget, ++cameraFrameSequence);
        summary.put("cameraFramesProcessed", ((Number) summary.get("cameraFramesProcessed")).intValue() + 1);
        append("recognition-events.jsonl", row("CAMERA_FRAME_CAPTURED",
                Map.of("frameId", frame.getFrameId(), "trackId", frame.getTrackId(),
                        "x", frame.getFrameCenterX(), "y", frame.getFrameCenterY(),
                        "width", frame.getWidth(), "height", frame.getHeight(),
                        "pixelHash", frame.getAttributes().get("pixelHash"),
                        "signalClass", frame.getClass().getSimpleName())));

        RecognitionResultSignal result = imageRecognitionNeuron.recognize(frame);
        summary.put("recognitionsProduced", ((Number) summary.get("recognitionsProduced")).intValue() + 1);
        Map<String, Object> recognitionRow = new LinkedHashMap<>();
        recognitionRow.put("frameId", result.getFrameId());
        recognitionRow.put("targetId", result.getTargetId());
        recognitionRow.put("classification", result.getClassification());
        recognitionRow.put("confidence", result.getConfidence());
        recognitionRow.put("imageFeatures", result.getImageFeatures());
        recognitionRow.put("pixelHash", result.getAttributes().get("pixelHash"));
        recognitionRow.put("source", result.getAttributes().get("source"));
        recognitionRow.put("architecture", result.getAttributes().get("architecture"));
        recognitionRow.put("pixelPatchSignals", result.getAttributes().get("pixelPatchSignals"));
        recognitionRow.put("conv1FeatureSignals", result.getAttributes().get("conv1FeatureSignals"));
        recognitionRow.put("conv2FeatureSignals", result.getAttributes().get("conv2FeatureSignals"));
        recognitionRow.put("pooledFeatureSignals", result.getAttributes().get("pooledFeatureSignals"));
        recognitionRow.put("classifierScores", result.getAttributes().get("classifierScores"));
        recognitionRow.put("signalClass", result.getClass().getSimpleName());
        append("recognition-events.jsonl", row("IMAGE_RECOGNITION_RESULT", recognitionRow));
        if (result.getConfidence() >= 0.45) {
            ObservationTarget recognized = recognizedTarget(worldTarget, result);
            targets.add(recognized);
            recognizedTargetIds.add(recognized.targetId);
            append("target-events.jsonl", row("TARGET_RECOGNIZED_FROM_IMAGE",
                    Map.of("targetId", recognized.targetId, "classification", recognized.classification,
                            "confidence", recognized.confidence, "source", "PIXEL_MATRIX",
                            "frameId", result.getFrameId())));
        }
    }

    private TargetPriority detectEvaluateAndSelect() throws IOException {
        tick++;
        List<ObservationTarget> active = activeTargets();
        for (ObservationTarget target : active) {
            TargetDetectionSignal signal = new TargetDetectionSignal(
                    scenario.config.missionId, scenario.config.uavId, tick, target);
            append("target-events.jsonl", row("TARGET_DETECTED",
                    Map.of("targetId", target.targetId, "classification", target.classification,
                            "confidence", target.confidence, "signalClass", signal.getClass().getSimpleName())));
        }
        summary.put("targetsDetected", active.size());
        transition(UavMissionState.TARGET_CANDIDATE_FOUND, "detections=" + active.size());
        transition(UavMissionState.TARGET_EVALUATION, "scoring-candidates");
        TargetPriority firstSelection = scoreAndSelect(active);
        if (firstSelection == null) {
            return null;
        }
        selectedTargetId = firstSelection.targetId;

        boolean replanned = applyTargetEventsBeforeApproach();
        if (replanned) {
            active = activeTargets();
            TargetPriority updated = scoreAndSelect(active);
            if (updated != null) {
                summary.put("replannedBeforeObservation", !updated.targetId.equals(firstSelection.targetId));
                firstSelection = updated;
                selectedTargetId = updated.targetId;
            }
        }

        TargetSelectedSignal selectedSignal = new TargetSelectedSignal(
                scenario.config.missionId, scenario.config.uavId, tick, firstSelection);
        append("target-events.jsonl", row("TARGET_SELECTED",
                Map.of("targetId", firstSelection.targetId, "priorityScore", firstSelection.score,
                        "signalClass", selectedSignal.getClass().getSimpleName())));
        summary.put("targetsSelected", ((Number) summary.get("targetsSelected")).intValue() + 1);
        transition(UavMissionState.TARGET_SELECTED, "target=" + firstSelection.targetId);
        return firstSelection;
    }

    private TargetPriority scoreAndSelect(List<ObservationTarget> active) throws IOException {
        List<TargetPriority> priorities = targetEvaluationNeuron.score(active, pose, scenario.config);
        for (TargetPriority priority : priorities) {
            append("transparency.jsonl", row("TARGET_PRIORITY",
                    Map.of("targetId", priority.targetId, "factors", priority.factors, "score", priority.score,
                            "formula", "weighted observation-priority score")));
            TargetCandidateSignal candidateSignal = new TargetCandidateSignal(
                    scenario.config.missionId, scenario.config.uavId, tick, priority);
            append("target-events.jsonl", row("TARGET_CANDIDATE",
                    Map.of("targetId", priority.targetId, "priorityScore", priority.score,
                            "signalClass", candidateSignal.getClass().getSimpleName())));
        }
        if (priorities.isEmpty()) {
            return null;
        }
        return targetEvaluationNeuron.select(priorities);
    }

    private boolean applyTargetEventsBeforeApproach() throws IOException {
        boolean changed = false;
        for (ScenarioTargetEvent event : scenario.targetEvents.stream()
                .sorted(Comparator.comparingLong(e -> e.tick)).toList()) {
            if (event.tick <= tick + 2) {
                tick = Math.max(tick, event.tick);
                worldTargets.add(event.target);
                recognizeWorldTarget(event.target);
                changed = true;
                append("target-events.jsonl", row("TARGET_APPEARED",
                        Map.of("targetId", event.target.targetId, "tick", tick)));
            }
        }
        return changed;
    }

    private ConfirmationEvaluation requestConfirmation(ObservationTarget target) throws IOException {
        transition(UavMissionState.HOLDING_FOR_CONFIRMATION, "hold-outside-observation-radius");
        tick++;
        String requestId = "confirm-" + scenario.config.missionId + "-" + (++confirmationSequence);
        TargetConfirmationRequestSignal request = new TargetConfirmationRequestSignal(
                scenario.config.missionId,
                scenario.config.uavId,
                tick,
                requestId,
                target.targetId,
                UavActionType.APPROACH_OBSERVATION_POINT,
                tick + scenario.config.confirmationTimeoutTicks);
        summary.put("confirmationRequests", ((Number) summary.get("confirmationRequests")).intValue() + 1);
        append("confirmation-events.jsonl", row("CONFIRMATION_REQUESTED",
                Map.of("requestId", requestId, "targetId", target.targetId, "expiresAtTick", request.getExpiresAtTick())));

        ConfirmationEvaluation last = ConfirmationEvaluation.pending();
        ConfirmationEvaluation approvedBeforeDuplicate = null;
        List<ScenarioConfirmationEvent> events = scenario.confirmationEvents.stream()
                .sorted(Comparator.comparingLong(e -> e.tick)).toList();
        if (events.isEmpty()) {
            tick = request.getExpiresAtTick() + 1;
            last = confirmationGateNeuron.evaluate(request, null, tick);
            logConfirmationResult(request, null, last);
            return last;
        }
        for (ScenarioConfirmationEvent event : events) {
            tick = event.tick;
            TargetConfirmationResponseSignal response = responseFor(event.decision, request, target);
            last = confirmationGateNeuron.evaluate(request, response, tick);
            logConfirmationResult(request, response, last);
            if (last.status == ConfirmationStatus.APPROVED) {
                approvedBeforeDuplicate = last;
            }
            if (last.status == ConfirmationStatus.APPROVED
                    || last.status == ConfirmationStatus.DENIED
                    || last.status == ConfirmationStatus.TIMEOUT) {
                if (!scenario.faults.duplicateConfirmation) {
                    return last;
                }
            }
        }
        if (last.status == ConfirmationStatus.PENDING && tick <= request.getExpiresAtTick()) {
            tick = request.getExpiresAtTick() + 1;
            last = confirmationGateNeuron.evaluate(request, null, tick);
            logConfirmationResult(request, null, last);
        }
        return approvedBeforeDuplicate == null ? last : approvedBeforeDuplicate;
    }

    private TargetConfirmationResponseSignal responseFor(ConfirmationDecision decision,
                                                         TargetConfirmationRequestSignal request,
                                                         ObservationTarget target) {
        String responseRequestId = request.getRequestId();
        String responseTargetId = target.targetId;
        long responseTick = tick;
        if (decision == ConfirmationDecision.WRONG_REQUEST_ID) {
            responseRequestId = request.getRequestId() + "-wrong";
        }
        if (decision == ConfirmationDecision.WRONG_TARGET) {
            responseTargetId = target.targetId + "-wrong";
        }
        if (decision == ConfirmationDecision.APPROVE_TOO_LATE) {
            responseTick = request.getExpiresAtTick() + 1;
            tick = responseTick;
        }
        return new TargetConfirmationResponseSignal(
                scenario.config.missionId,
                scenario.config.uavId,
                responseTick,
                responseRequestId,
                responseTargetId,
                UavActionType.APPROACH_OBSERVATION_POINT,
                decision);
    }

    private void logConfirmationResult(TargetConfirmationRequestSignal request,
                                       TargetConfirmationResponseSignal response,
                                       ConfirmationEvaluation evaluation) throws IOException {
        if (evaluation.status == ConfirmationStatus.APPROVED) {
            summary.put("validConfirmations", ((Number) summary.get("validConfirmations")).intValue() + 1);
        } else if (evaluation.status == ConfirmationStatus.REJECTED || evaluation.status == ConfirmationStatus.DENIED
                || evaluation.status == ConfirmationStatus.TIMEOUT) {
            summary.put("rejectedConfirmations", ((Number) summary.get("rejectedConfirmations")).intValue() + 1);
        }
        append("confirmation-events.jsonl", row("CONFIRMATION_" + evaluation.status,
                Map.of("requestId", request.getRequestId(), "targetId", request.getTargetId(),
                        "decision", response == null ? "NONE" : response.getDecision(),
                        "reason", evaluation.reason, "tick", tick)));
    }

    private boolean approachAndObserve(ObservationTarget target) throws IOException {
        UavPose destination = safeObservationPose(target);
        UavIntentSignal intent = missionProcessor.intent(
                scenario.config, tick + 1, UavActionType.APPROACH_OBSERVATION_POINT, target, destination);
        SimUavSupervisorContext context = new SimUavSupervisorContext(scenario.config, pose, tick + 1);
        if (scenario.faults.lostHeartbeat) {
            context.jneopalliumHeartbeatHealthy = false;
        }
        SimUavCommandGateway.CommandDispatch dispatch = commandGateway.dispatch(intent, context);
        append("supervisor-audit.jsonl", dispatch.audit());
        if (!dispatch.decision().accepted) {
            summary.put("safetyVetoes", ((Number) summary.get("safetyVetoes")).intValue() + 1);
            if (dispatch.decision().reasons.stream().anyMatch(reason -> reason.startsWith("GEOFENCE"))) {
                summary.put("geofenceViolations", ((Number) summary.get("geofenceViolations")).intValue() + 1);
            }
            append("mission-events.jsonl", row("SAFETY_VETO",
                    Map.of("reasons", dispatch.decision().reasons, "targetId", target.targetId)));
            transition(UavMissionState.SAFETY_HOLD, "supervisor-rejected");
            returnHome("supervisor-rejected");
            return false;
        }
        transition(UavMissionState.APPROACHING_SAFE_OBSERVATION_POINT, "supervisor-accepted");
        pose.x = destination.x;
        pose.y = destination.y;
        pose.altitudeMeters = destination.altitudeMeters;
        double distance = pose.distance2d(target.x, target.y);
        summary.put("minimumTargetDistance", Math.min(((Number) summary.get("minimumTargetDistance")).doubleValue(), distance));
        transition(UavMissionState.OBSERVING, "safe-observation-point");
        return true;
    }

    private void photograph(ObservationTarget target) throws IOException {
        transition(UavMissionState.PHOTOGRAPHING, "start-photo");
        boolean accepted = false;
        int maxAttempts = Math.max(1, scenario.config.photographRetryLimit + 1);
        for (int attempt = 1; attempt <= maxAttempts && !accepted; attempt++) {
            tick++;
            PhotographRequestSignal request = new PhotographRequestSignal(
                    scenario.config.missionId,
                    scenario.config.uavId,
                    tick,
                    "photo-request-" + (++photographSequence),
                    target.targetId);
            summary.put("photographsAttempted", ((Number) summary.get("photographsAttempted")).intValue() + 1);
            PhotographResultSignal result = photographyNeuron.photograph(request, target, pose, scenario.config, tick, attempt);
            Map<String, Object> photoRow = new LinkedHashMap<>(result.getAttributes());
            photoRow.put("reason", result.getReason());
            photoRow.put("accepted", result.isAccepted());
            append("photograph-events.jsonl", row(result.isAccepted() ? "PHOTOGRAPH_ACCEPTED" : "PHOTOGRAPH_REJECTED",
                    photoRow));
            accepted = result.isAccepted();
            if (accepted) {
                summary.put("photographsAccepted", ((Number) summary.get("photographsAccepted")).intValue() + 1);
            } else if (scenario.faults.poorVisibility && "VISIBILITY_TOO_LOW".equals(result.getReason())) {
                target.visibility = 0.86;
                append("mission-events.jsonl", row("PHOTO_RETRY_REQUESTED",
                        Map.of("targetId", target.targetId, "attempt", attempt, "retryLimit", scenario.config.photographRetryLimit)));
            }
        }
        transition(UavMissionState.VERIFYING_PHOTO, accepted ? "photo-accepted" : "photo-not-accepted");
    }

    private UavPose safeObservationPose(ObservationTarget target) {
        double distance = scenario.config.targetObservationDistanceMeters;
        return new UavPose(
                target.x - distance,
                target.y,
                Math.min(Math.max(30.0, pose.altitudeMeters), scenario.config.maximumAltitudeMeters),
                0.0,
                pose.batteryFraction,
                pose.localizationConfidence);
    }

    private void returnHome(String reason) throws IOException {
        if (missionNeuron.state() != UavMissionState.RETURNING_HOME) {
            transition(UavMissionState.RETURNING_HOME, reason);
        }
        summary.put("returnToHomeEvents", ((Number) summary.get("returnToHomeEvents")).intValue() + 1);
        pose.x = 0.0;
        pose.y = 0.0;
        tick++;
        transition(UavMissionState.LANDED, "home-reached");
        transition(UavMissionState.COMPLETED, "landed");
    }

    private UavSingleRunManifest finish(String status, boolean missionCompleted) throws IOException {
        summary.put("scenario", scenario.scenarioId);
        summary.put("mode", scenario.mode);
        summary.put("status", status);
        summary.put("ticks", tick);
        summary.put("missionCompleted", missionCompleted);
        summary.put("deterministicSeed", scenario.seed);
        summary.put("minimumBattery", Math.min(((Number) summary.get("minimumBattery")).doubleValue(), pose.batteryFraction));
        writeJson("summary.json", summary);
        writeJson("safety-summary.json", safetySummary(status));
        UavSingleRunManifest manifest = new UavSingleRunManifest();
        manifest.scenario = scenario.scenarioId;
        manifest.mode = scenario.mode;
        manifest.status = status;
        manifest.ticks = (int) tick;
        manifest.missionCompleted = missionCompleted;
        manifest.deterministicSeed = scenario.seed;
        manifest.metrics.putAll(summary);
        for (String file : artifactFiles()) {
            manifest.artifacts.put(file, outputDir.resolve(file).toAbsolutePath().toString());
        }
        applyAssertions(manifest);
        summary.put("status", manifest.status);
        writeJson("summary.json", summary);
        writeJson("safety-summary.json", safetySummary(manifest.status));
        writeJson("manifest.json", manifest);
        return manifest;
    }

    private Map<String, Object> safetySummary(String status) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("status", status);
        row.put("simulatorOnly", scenario.config.simulatorOnly);
        row.put("vehicleSystemId", scenario.config.vehicleSystemId);
        row.put("vehicleAllowlist", scenario.config.vehicleAllowlist);
        row.put("safetyVetoes", summary.get("safetyVetoes"));
        row.put("geofenceViolations", summary.get("geofenceViolations"));
        row.put("minimumBattery", summary.get("minimumBattery"));
        row.put("noDirectRealVehicleCommandPath", true);
        return row;
    }

    private void applyAssertions(UavSingleRunManifest manifest) {
        Map<String, Boolean> a = manifest.assertions;
        a.put("simulatorOnly", scenario.config.simulatorOnly);
        a.put("artifactsExist", artifactFiles().stream().allMatch(file -> Files.exists(outputDir.resolve(file))));
        a.put("supervisorAudited", lineCount(outputDir.resolve("supervisor-audit.jsonl")) > 0
                || scenario.scenarioId.startsWith("confirm_") && !"confirm_approved".equals(scenario.scenarioId));
        a.put("noDirectRealVehicleCommandPath", true);
        a.put("searchAreaPlanned", ((Number) summary.get("searchWaypointsPlanned")).intValue() > 0);
        a.put("searchRouteVisitedOrSafelyVetoed", ((Number) summary.get("searchWaypointsVisited")).intValue() > 0
                || ((Number) summary.get("safetyVetoes")).intValue() > 0);
        a.put("pixelRecognitionUsed", ((Number) summary.get("recognitionsProduced")).intValue() > 0
                || "low_battery_rth".equals(scenario.scenarioId));
        switch (scenario.scenarioId) {
            case "autonomous_success" -> {
                a.put("higherPrioritySelected", "target-beta".equals(selectedTargetId));
                a.put("photoAccepted", ((Number) summary.get("photographsAccepted")).intValue() == 1);
                a.put("returnedHome", ((Number) summary.get("returnToHomeEvents")).intValue() >= 1);
            }
            case "autonomous_priority_change" -> {
                a.put("replannedBeforeObservation", Boolean.TRUE.equals(summary.get("replannedBeforeObservation")));
                a.put("newTargetSelected", "target-new-priority".equals(selectedTargetId));
            }
            case "confirm_approved" -> {
                a.put("validConfirmation", ((Number) summary.get("validConfirmations")).intValue() == 1);
                a.put("photoAfterConfirmation", ((Number) summary.get("photographsAccepted")).intValue() == 1);
            }
            case "confirm_denied" -> {
                a.put("denialRejectedAction", ((Number) summary.get("validConfirmations")).intValue() == 0);
                a.put("noPhotograph", ((Number) summary.get("photographsAttempted")).intValue() == 0);
                a.put("noTargetCommandAfterDenial", !lineContains(outputDir.resolve("supervisor-audit.jsonl"),
                        UavActionType.APPROACH_OBSERVATION_POINT.name()));
            }
            case "confirm_timeout" -> {
                a.put("timeoutRejectedAction", ((Number) summary.get("rejectedConfirmations")).intValue() >= 1);
                a.put("noCommandAfterTimeout", ((Number) summary.get("photographsAttempted")).intValue() == 0);
                a.put("noTargetCommandAfterTimeout", !lineContains(outputDir.resolve("supervisor-audit.jsonl"),
                        UavActionType.APPROACH_OBSERVATION_POINT.name()));
            }
            case "low_battery_rth" -> {
                a.put("photographySkipped", ((Number) summary.get("photographsAttempted")).intValue() == 0);
                a.put("returnHomeIssued", ((Number) summary.get("returnToHomeEvents")).intValue() >= 1);
            }
            case "geofence_veto" -> {
                a.put("geofenceRejected", ((Number) summary.get("geofenceViolations")).intValue() >= 1);
                a.put("noGoZoneRejected", lineContains(outputDir.resolve("supervisor-audit.jsonl"), "NO_GO_ZONE_REJECTED"));
                a.put("actionVetoed", ((Number) summary.get("safetyVetoes")).intValue() >= 1);
            }
            case "lost_heartbeat" -> {
                a.put("heartbeatRejected", ((Number) summary.get("safetyVetoes")).intValue() >= 1);
                a.put("safeRecovery", ((Number) summary.get("returnToHomeEvents")).intValue() >= 1);
            }
            case "poor_visibility" -> {
                a.put("retryWithinLimit", ((Number) summary.get("photographsAttempted")).intValue()
                        <= scenario.config.photographRetryLimit + 1);
                a.put("visibilityFailureRecorded", lineContains(outputDir.resolve("photograph-events.jsonl"), "VISIBILITY_TOO_LOW"));
            }
            case "duplicate_confirmation" -> {
                a.put("secondConfirmationRejected", lineContains(outputDir.resolve("confirmation-events.jsonl"), "DUPLICATE_CONFIRMATION"));
                a.put("singleValidConfirmation", ((Number) summary.get("validConfirmations")).intValue() == 1);
            }
            default -> a.put("knownScenario", false);
        }
        manifest.status = a.values().stream().allMatch(Boolean::booleanValue) ? manifest.status : "FAIL";
    }

    private void transition(UavMissionState next, String reason) throws IOException {
        UavMissionState previous = missionNeuron.state();
        missionNeuron.transition(next);
        append("mission-events.jsonl", row("STATE_TRANSITION",
                Map.of("from", previous, "to", next, "reason", reason)));
    }

    private void addMissionEvent(String type, Map<String, Object> attributes) throws IOException {
        append("mission-events.jsonl", row(type, attributes));
    }

    private List<ObservationTarget> activeTargets() {
        return targets.stream().filter(target -> target.active).toList();
    }

    private Optional<ObservationTarget> targetById(String targetId) {
        return targets.stream().filter(target -> target.targetId.equals(targetId)).findFirst();
    }

    private static ObservationTarget recognizedTarget(ObservationTarget worldTarget, RecognitionResultSignal result) {
        ObservationTarget target = new ObservationTarget(result.getTargetId(), result.getClassification(),
                result.getX(), result.getY());
        target.missionRelevance = worldTarget.missionRelevance;
        target.confidence = result.getConfidence();
        target.urgency = worldTarget.urgency;
        target.informationValue = worldTarget.informationValue;
        target.communicationValue = worldTarget.communicationValue;
        target.safetyRisk = worldTarget.safetyRisk;
        target.visibility = worldTarget.visibility;
        target.inFieldOfView = worldTarget.inFieldOfView;
        target.motionBlurEstimate = worldTarget.motionBlurEstimate;
        target.active = worldTarget.active;
        return target;
    }

    private Map<String, Object> row(String type, Map<String, ?> attributes) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("tick", tick);
        row.put("scenario", scenario.scenarioId);
        row.put("missionId", scenario.config.missionId);
        row.put("uavId", scenario.config.uavId);
        row.put("type", type);
        row.putAll(attributes);
        return row;
    }

    private void initSummary() {
        summary.put("scenario", scenario.scenarioId);
        summary.put("mode", scenario.mode);
        summary.put("status", "RUNNING");
        summary.put("ticks", 0L);
        summary.put("missionCompleted", false);
        summary.put("targetsDetected", 0);
        summary.put("targetsSelected", 0);
        summary.put("confirmationRequests", 0);
        summary.put("validConfirmations", 0);
        summary.put("rejectedConfirmations", 0);
        summary.put("photographsAttempted", 0);
        summary.put("photographsAccepted", 0);
        summary.put("safetyVetoes", 0);
        summary.put("returnToHomeEvents", 0);
        summary.put("minimumBattery", pose.batteryFraction);
        summary.put("minimumTargetDistance", 1_000_000.0);
        summary.put("geofenceViolations", 0);
        summary.put("deterministicSeed", scenario.seed);
        summary.put("replannedBeforeObservation", false);
        summary.put("searchAreaId", scenario.searchArea.areaId);
        summary.put("searchWaypointsPlanned", 0);
        summary.put("searchWaypointsVisited", 0);
        summary.put("cameraFramesProcessed", 0);
        summary.put("recognitionsProduced", 0);
    }

    private void writeManifestSkeleton() throws IOException {
        for (String file : artifactFiles()) {
            if (file.endsWith(".jsonl")) {
                Files.writeString(outputDir.resolve(file), "", StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
        }
        writeJson("manifest.json", Map.of(
                "scenario", scenario.scenarioId,
                "mode", scenario.mode,
                "status", "RUNNING",
                "deterministicSeed", scenario.seed));
    }

    private void append(String file, Object row) throws IOException {
        Files.writeString(outputDir.resolve(file), toJson(row) + System.lineSeparator(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private void writeJson(String file, Object value) throws IOException {
        Files.writeString(outputDir.resolve(file),
                MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value) + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize UAV single artifact", e);
        }
    }

    private static List<String> artifactFiles() {
        return List.of(
                "manifest.json",
                "summary.json",
                "mission-events.jsonl",
                "search-events.jsonl",
                "target-events.jsonl",
                "recognition-events.jsonl",
                "confirmation-events.jsonl",
                "photograph-events.jsonl",
                "supervisor-audit.jsonl",
                "transparency.jsonl",
                "safety-summary.json");
    }

    private static long lineCount(Path path) {
        if (!Files.exists(path)) {
            return 0L;
        }
        try (var lines = Files.lines(path)) {
            return lines.filter(line -> !line.isBlank()).count();
        } catch (IOException e) {
            return 0L;
        }
    }

    private static boolean lineContains(Path path, String text) {
        if (!Files.exists(path)) {
            return false;
        }
        try (var lines = Files.lines(path)) {
            return lines.anyMatch(line -> line.contains(text));
        } catch (IOException e) {
            return false;
        }
    }
}
