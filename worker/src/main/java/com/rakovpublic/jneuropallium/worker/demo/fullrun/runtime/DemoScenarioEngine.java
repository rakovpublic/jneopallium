package com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime;

import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class DemoScenarioEngine {
    private DemoScenarioEngine() {
    }

    public static List<IInputSignal> inputSignals(String demoId, long tick) {
        return switch (demoId) {
            case "demo-01-industrial-control" -> industrialInputs(demoId, tick);
            case "demo-02-pump-fleet-maintenance" -> pumpInputs(demoId, tick);
            case "demo-03-drone-mavlink-guard" -> droneInputs(demoId, tick);
            case "demo-04-clinical-fhir-advisory" -> clinicalInputs(demoId, tick);
            case "demo-05-dicom-readonly-context" -> dicomInputs(demoId, tick);
            case "demo-06-cybersecurity-kafka-triage" -> securityInputs(demoId, tick);
            case "demo-07-observability-otel-export" -> observabilityInputs(demoId, tick);
            case "demo-08-adaptive-tutoring-lti" -> tutoringInputs(demoId, tick);
            case "demo-09-nengo-interop" -> nengoInputs(demoId, tick);
            default -> throw new IllegalArgumentException("Unknown demo id " + demoId);
        };
    }

    public static void applyStage(String demoId, String stage, DemoSignal input, DemoSignal output) {
        DemoDefinition definition = DemoCatalog.get(demoId);
        output.setDemoId(demoId);
        output.setMode(definition.safetyMode());
        output.setSignalType(stage);
        output.setConfidence(0.90);
        switch (demoId) {
            case "demo-01-industrial-control" -> industrialStage(input, output);
            case "demo-02-pump-fleet-maintenance" -> pumpStage(input, output);
            case "demo-03-drone-mavlink-guard" -> droneStage(input, output);
            case "demo-04-clinical-fhir-advisory" -> clinicalStage(input, output);
            case "demo-05-dicom-readonly-context" -> dicomStage(input, output);
            case "demo-06-cybersecurity-kafka-triage" -> securityStage(input, output);
            case "demo-07-observability-otel-export" -> observabilityStage(input, output);
            case "demo-08-adaptive-tutoring-lti" -> tutoringStage(input, output);
            case "demo-09-nengo-interop" -> nengoStage(input, output);
            default -> throw new IllegalArgumentException("Unknown demo id " + demoId);
        }
        output.withAttribute("stage", stage);
    }

    private static List<IInputSignal> industrialInputs(String demoId, long tick) {
        double temperature = tick == 5 ? 126.0 : 88.0 + (tick % 10) * 0.35;
        double flow = 42.0 - (tick % 6) * 0.4;
        double valveFeedback = 0.44 + (tick % 5) * 0.02;
        boolean alarm = tick == 5;
        boolean manualOverride = tick == 10;
        Map<String, String> attrs = attrs(
                "temperature", temperature,
                "flow", flow,
                "valveFeedback", valveFeedback,
                "alarm", alarm,
                "manualOverride", manualOverride,
                "targetTemperature", 85.0);
        return List.of(
                signal(demoId, tick, "reactor-a", "TemperatureSignal", temperature, attrs),
                signal(demoId, tick, "reactor-a", "FlowSignal", flow, attrs),
                signal(demoId, tick, "reactor-a", "AlarmSignal", alarm ? 1.0 : 0.0, attrs)
        );
    }

    private static List<IInputSignal> pumpInputs(String demoId, long tick) {
        List<IInputSignal> signals = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String pump = "pump-" + String.format(Locale.ROOT, "%02d", i);
            boolean degrading = i == 3;
            boolean offline = i == 7 && tick >= 8;
            double vibration = degrading ? 0.25 + tick * 0.01 : 0.22 + (i * 0.01);
            double bearingTemp = degrading ? 60.0 + tick * 0.09 : 48.0 + i;
            double rul = offline ? 0.0 : degrading ? Math.max(40.0, 900.0 - tick * 30.0) : 950.0 - i * 4.0;
            Map<String, String> attrs = attrs(
                    "vibration", vibration,
                    "bearingTemp", bearingTemp,
                    "online", !offline,
                    "degrading", degrading,
                    "rul", rul,
                    "assetType", "pump");
            signals.add(signal(demoId, tick, pump, "VibrationSignal", vibration, attrs));
            signals.add(signal(demoId, tick, pump, "BearingTempSignal", bearingTemp, attrs));
            if (offline || tick % 100 == 0) {
                signals.add(signal(demoId, tick, pump, "DeviceEventSignal", offline ? 1.0 : 0.0, attrs));
            }
        }
        return signals;
    }

    private static List<IInputSignal> droneInputs(String demoId, long tick) {
        double battery = tick >= 12 ? 18.0 : 82.0 - tick * 0.4;
        double gps = tick == 12 ? 0.6 : 0.95;
        double altitude = 120.0 + tick * 0.6;
        double geofence = tick == 6 ? -3.0 : 55.0 - tick * 0.5;
        String command = tick == 6 ? "EXIT_GEOFENCE" : "MISSION_WAYPOINT";
        Map<String, String> attrs = attrs("battery", battery, "gpsQuality", gps, "altitude", altitude,
                "geofenceDistance", geofence, "command", command);
        return List.of(
                signal(demoId, tick, "drone-sim-1", "BatterySignal", battery, attrs),
                signal(demoId, tick, "drone-sim-1", "GpsSignal", gps, attrs),
                signal(demoId, tick, "drone-sim-1", "AltitudeSignal", altitude, attrs),
                signal(demoId, tick, "drone-sim-1", "GeofenceSignal", geofence, attrs),
                signal(demoId, tick, "drone-sim-1", "MissionCommandSignal", geofence, attrs)
        );
    }

    private static List<IInputSignal> clinicalInputs(String demoId, long tick) {
        Map<String, String> stable = attrs("heartRate", 78, "spo2", 97, "temperature", 36.8,
                "medication", "none", "highRisk", false);
        Map<String, String> high = attrs("heartRate", 128, "spo2", 88, "temperature", 39.3,
                "medication", "beta-blocker", "highRisk", true);
        return List.of(
                signal(demoId, tick, "patient-stable", "VitalSignal", 0.1, stable),
                signal(demoId, tick, "patient-high-risk", "VitalSignal", 0.9, high),
                signal(demoId, tick, "patient-high-risk", "MedicationContextSignal", 0.8, high)
        );
    }

    private static List<IInputSignal> dicomInputs(String demoId, long tick) {
        Map<String, String> complete = attrs("modality", "CT", "bodyPart", "CHEST", "seriesCount", 4,
                "studyAgeHours", 2, "accessionPresent", true, "studyPresent", true);
        Map<String, String> missing = attrs("modality", "MR", "bodyPart", "", "seriesCount", 1,
                "studyAgeHours", 96, "accessionPresent", false, "studyPresent", false);
        return List.of(
                signal(demoId, tick, "study-complete", "DicomMetadataSignal", 0.2, complete),
                signal(demoId, tick, "study-missing-metadata", "DicomMetadataSignal", 0.9, missing)
        );
    }

    private static List<IInputSignal> securityInputs(String demoId, long tick) {
        double attackFailures = tick >= 5 && tick <= 15 ? 38.0 : 3.0;
        Map<String, String> attack = attrs("scenario", "attack-like", "authFailures", attackFailures,
                "sourceReputation", 0.12, "endpointCriticality", 0.9, "eventType", "auth-failure-burst",
                "serviceAccount", false);
        Map<String, String> benign = attrs("scenario", "benign", "authFailures", 18.0,
                "sourceReputation", 0.85, "endpointCriticality", 0.35, "eventType", "service-retry",
                "serviceAccount", true);
        return List.of(
                signal(demoId, tick, "endpoint-attack", "SecurityEventSignal", attackFailures, attack),
                signal(demoId, tick, "svc-backup", "SecurityEventSignal", 18.0, benign)
        );
    }

    private static List<IInputSignal> observabilityInputs(String demoId, long tick) {
        boolean spike = tick >= 6 && tick <= 12;
        double latency = spike ? 850.0 : 120.0 + tick % 10;
        double errorRate = spike ? 0.12 : 0.005;
        double saturation = spike ? 0.88 : 0.45;
        Map<String, String> attrs = attrs("latencyP95", latency, "errorRate", errorRate, "saturation", saturation,
                "traceSpanFailure", spike, "windowStart", 6, "windowEnd", 12);
        return List.of(
                signal(demoId, tick, "service-checkout", "MetricSignal", latency, attrs),
                signal(demoId, tick, "service-checkout", "TraceSignal", spike ? 1.0 : 0.0, attrs),
                signal(demoId, tick, "service-checkout", "LogEventSignal", errorRate, attrs)
        );
    }

    private static List<IInputSignal> tutoringInputs(String demoId, long tick) {
        Map<String, String> struggling = attrs("learner", "struggling", "correctness", false,
                "responseTimeMs", 19000, "hintRequest", tick % 3 == 0, "topicId", "fractions");
        Map<String, String> strong = attrs("learner", "strong", "correctness", true,
                "responseTimeMs", 3200, "hintRequest", false, "topicId", "fractions");
        return List.of(
                signal(demoId, tick, "learner-struggling", "LearnerEventSignal", 0.2, struggling),
                signal(demoId, tick, "learner-strong", "LearnerEventSignal", 0.95, strong)
        );
    }

    private static List<IInputSignal> nengoInputs(String demoId, long tick) {
        double x = Math.sin(tick / 10.0);
        double y = Math.cos(tick / 10.0);
        Map<String, String> attrs = attrs("vector", String.format(Locale.ROOT, "[%.4f,%.4f]", x, y),
                "timestamp", tick, "channelCount", 2);
        return List.of(signal(demoId, tick, "nengo-mock-stream", "NengoVectorSignal", x + y, attrs));
    }

    private static void industrialStage(DemoSignal input, DemoSignal output) {
        boolean alarm = bool(input, "alarm");
        boolean override = bool(input, "manualOverride");
        double temperature = dbl(input, "temperature");
        if (alarm) {
            output.setResultType("FAIL_SAFE_COMMAND");
            output.setDecision("FAIL_SAFE");
            output.setReason("high-temperature alarm forced fail-safe cooling");
            output.setNumericValue(1.0);
            output.setConfidence(0.99);
        } else if (override) {
            output.setResultType("HELD_COMMAND");
            output.setDecision("HOLD_REJECTED");
            output.setReason("manual override held command for operator review");
            output.setNumericValue(0.0);
            output.setConfidence(0.96);
        } else {
            double demand = clamp((temperature - 82.0) / 25.0, 0.0, 1.0);
            output.setResultType("VALVE_COMMAND");
            output.setDecision("ALLOW_COOLING_ADJUSTMENT");
            output.setReason("normal cascade cooling demand");
            output.setNumericValue(demand);
            output.setConfidence(0.91);
        }
    }

    private static void pumpStage(DemoSignal input, DemoSignal output) {
        boolean online = bool(input, "online");
        boolean degrading = bool(input, "degrading");
        double rul = dbl(input, "rul");
        if (!online) {
            output.setResultType("DEVICE_OFFLINE_ADVISORY");
            output.setDecision("RAISE_OFFLINE_ALARM");
            output.setReason("pump telemetry reports offline event");
            output.setNumericValue(0.0);
            output.setConfidence(0.98);
        } else if (degrading && rul < 840.0) {
            output.setResultType("MAINTENANCE_ADVISORY");
            output.setDecision("PROPOSE_MAINTENANCE_WINDOW");
            output.setReason("rising vibration trend reduced remaining useful life");
            output.setNumericValue(rul);
            output.setConfidence(0.94);
        } else {
            output.setResultType("HEALTH_OK");
            output.setDecision("NO_MAINTENANCE_ADVISORY");
            output.setReason("pump trend remains inside advisory band");
            output.setNumericValue(rul);
            output.setConfidence(0.88);
        }
    }

    private static void droneStage(DemoSignal input, DemoSignal output) {
        double geofence = dbl(input, "geofenceDistance");
        double battery = dbl(input, "battery");
        if (geofence < 0.0) {
            output.setResultType("COMMAND_VETO");
            output.setDecision("VETO_GEOFENCE_VIOLATION");
            output.setReason("mission command would leave geofence");
            output.setNumericValue(1.0);
            output.setConfidence(0.99);
        } else if (battery < 25.0) {
            output.setResultType("RETURN_TO_HOME_ADVISORY");
            output.setDecision("RECOMMEND_RTH");
            output.setReason("low battery risk in simulated mission");
            output.setNumericValue(battery);
            output.setConfidence(0.95);
        } else {
            output.setResultType("COMMAND_ALLOWED");
            output.setDecision("ALLOW_COMMAND");
            output.setReason("mission command stays inside simulated safety envelope");
            output.setNumericValue(0.0);
            output.setConfidence(0.90);
        }
    }

    private static void clinicalStage(DemoSignal input, DemoSignal output) {
        if (bool(input, "highRisk")) {
            output.setResultType("CLINICIAN_REVIEW_ADVISORY");
            output.setDecision("RECOMMEND_CLINICIAN_REVIEW");
            output.setReason("synthetic vitals show abnormal heart rate, oxygen saturation, and temperature");
            output.setNumericValue(0.92);
            output.setConfidence(0.93);
        } else {
            output.setResultType("OBSERVATION_ONLY");
            output.setDecision("CONTINUE_MONITORING");
            output.setReason("synthetic vitals remain inside low-risk advisory band");
            output.setNumericValue(0.12);
            output.setConfidence(0.86);
        }
    }

    private static void dicomStage(DemoSignal input, DemoSignal output) {
        boolean missing = !bool(input, "accessionPresent") || !bool(input, "studyPresent");
        if (missing) {
            output.setResultType("QC_ADVISORY");
            output.setDecision("FLAG_MISSING_METADATA");
            output.setReason("missing accession or study metadata requires read-only quality-control review");
            output.setNumericValue(1.0);
            output.setConfidence(0.97);
        } else {
            output.setResultType("ROUTING_ADVISORY");
            output.setDecision("ROUTE_STANDARD_REVIEW");
            output.setReason("metadata complete; no pixel diagnosis performed");
            output.setNumericValue(0.2);
            output.setConfidence(0.89);
        }
    }

    private static void securityStage(DemoSignal input, DemoSignal output) {
        double failures = dbl(input, "authFailures");
        double reputation = dbl(input, "sourceReputation");
        boolean serviceAccount = bool(input, "serviceAccount");
        double score = serviceAccount ? failures * 0.01 : failures * (1.0 - reputation) / 40.0;
        output.withAttribute("score", score);
        if (score > 0.6) {
            output.setResultType("SECURITY_ADVISORY");
            output.setDecision("INVESTIGATE_BRUTE_FORCE");
            output.setReason("failed login burst with low source reputation raised attack hypothesis");
            output.setNumericValue(score);
            output.setConfidence(0.95);
        } else {
            output.setResultType("DAMPENED_BENIGN_PATTERN");
            output.setDecision("ADVISORY_DAMPEN_FALSE_POSITIVE");
            output.setReason("service-account retry pattern dampened as advisory-only false-positive control");
            output.setNumericValue(score);
            output.setConfidence(0.87);
        }
    }

    private static void observabilityStage(DemoSignal input, DemoSignal output) {
        boolean spike = bool(input, "traceSpanFailure") || dbl(input, "errorRate") > 0.05;
        output.withAttribute("anomalyWindowStart", input.getAttributes().get("windowStart"));
        output.withAttribute("anomalyWindowEnd", input.getAttributes().get("windowEnd"));
        if (spike) {
            output.setResultType("ANOMALY_SUMMARY");
            output.setDecision("EXPORT_OTEL_RECORD");
            output.setReason("latency and error-rate spike created export-only anomaly summary");
            output.setNumericValue(dbl(input, "latencyP95"));
            output.setConfidence(0.94);
        } else {
            output.setResultType("NORMAL_TELEMETRY_EXPORT");
            output.setDecision("EXPORT_BASELINE_RECORD");
            output.setReason("metrics remain inside baseline");
            output.setNumericValue(dbl(input, "latencyP95"));
            output.setConfidence(0.84);
        }
    }

    private static void tutoringStage(DemoSignal input, DemoSignal output) {
        boolean correct = bool(input, "correctness");
        boolean hint = bool(input, "hintRequest");
        double response = dbl(input, "responseTimeMs");
        if (!correct && (hint || response > 12000.0)) {
            output.setResultType("TUTOR_ADVISORY");
            output.setDecision("RECOMMEND_HINT_AND_LOWER_DIFFICULTY");
            output.setReason("repeated wrong answer pattern with long response time");
            output.setNumericValue(0.25);
            output.setConfidence(0.92);
        } else if (correct && response < 6000.0) {
            output.setResultType("TUTOR_ADVISORY");
            output.setDecision("RECOMMEND_HARDER_EXERCISE");
            output.setReason("strong performance supports increased exercise difficulty");
            output.setNumericValue(0.88);
            output.setConfidence(0.90);
        } else {
            output.setResultType("TUTOR_ADVISORY");
            output.setDecision("KEEP_CURRENT_DIFFICULTY");
            output.setReason("learner state remains stable");
            output.setNumericValue(0.55);
            output.setConfidence(0.82);
        }
    }

    private static void nengoStage(DemoSignal input, DemoSignal output) {
        double value = input.getNumericValue();
        output.setResultType("NENGO_VECTOR_OUTPUT");
        output.setDecision(value >= 0.0 ? "CLASS_POSITIVE_VECTOR" : "CLASS_NEGATIVE_VECTOR");
        output.setReason("mock Nengo vector stream transformed by Jneopallium local model");
        output.setNumericValue(value);
        output.setConfidence(0.87);
        output.withAttribute("outputVector", String.format(Locale.ROOT, "[%.4f,%.4f]", value, value / 2.0));
    }

    private static IInputSignal signal(String demoId, long tick, String entityId, String simpleSignalName,
                                       double value, Map<String, String> attrs) {
        DemoDefinition definition = DemoCatalog.get(demoId);
        String className = definition.packageName() + ".signals." + simpleSignalName;
        try {
            DemoSignal signal = (DemoSignal) Class.forName(className).getDeclaredConstructor().newInstance();
            signal.setDemoId(demoId);
            signal.setTick(tick);
            signal.setEntityId(entityId);
            signal.setSignalType(simpleSignalName);
            signal.setResultType("INPUT");
            signal.setNumericValue(value);
            signal.setConfidence(1.0);
            signal.setMode(definition.safetyMode());
            signal.setDecision("INPUT");
            signal.setReason("deterministic synthetic input");
            signal.setAttributes(attrs);
            signal.withAttribute("tick", tick);
            signal.withAttribute("entityId", entityId);
            signal.withAttribute("inputSignal", simpleSignalName);
            return signal;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException | ClassNotFoundException e) {
            throw new IllegalStateException("Cannot create input signal " + className, e);
        }
    }

    private static Map<String, String> attrs(Object... keyValues) {
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            result.put(String.valueOf(keyValues[i]), String.valueOf(keyValues[i + 1]));
        }
        return result;
    }

    private static double dbl(DemoSignal signal, String key) {
        String value = signal.getAttributes().get(key);
        return value == null || value.isBlank() ? 0.0 : Double.parseDouble(value);
    }

    private static boolean bool(DemoSignal signal, String key) {
        String value = signal.getAttributes().get(key);
        return Boolean.parseBoolean(value);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
