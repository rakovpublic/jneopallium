package com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DemoCatalog {
    private static final String BASE = "com.rakovpublic.jneuropallium.worker.demo.fullrun";
    private static final Map<String, DemoDefinition> DEMOS = new LinkedHashMap<>();

    static {
        add(def("demo-01-industrial-control", "demo01", "Industrial process control", "AUTONOMOUS-MOCK", 200,
                List.of("TemperatureSignal", "FlowSignal", "AlarmSignal", "ControlDemandSignal", "ValveCommandSignal", "SafetyVetoSignal"),
                List.of("ControlDemandSignal", "ValveCommandSignal", "SafetyVetoSignal", "ValveCommandSignal", "ValveCommandSignal"),
                List.of("sensor-normalize", "cascade-control", "interlock-rule", "safety-gate", "result-conversion"),
                List.of(4, 3, 2, 2, 3),
                "IndustrialDemoInput", "IndustrialControlNeuron", "IndustrialControlResultNeuron", "IndustrialControlProcessor"));

        add(def("demo-02-pump-fleet-maintenance", "demo02", "Predictive pump fleet maintenance", "ADVISORY", 1000,
                List.of("VibrationSignal", "BearingTempSignal", "DeviceEventSignal", "DegradationSignal", "RemainingUsefulLifeSignal", "MaintenanceAdvisorySignal"),
                List.of("DegradationSignal", "RemainingUsefulLifeSignal", "MaintenanceAdvisorySignal", "MaintenanceAdvisorySignal", "MaintenanceAdvisorySignal"),
                List.of("pump-feature", "health-estimation", "advisory-planning", "priority-score", "result-conversion"),
                List.of(5, 4, 3, 2, 3),
                "PumpFleetDemoInput", "PumpFleetNeuron", "PumpFleetResultNeuron", "PumpFleetProcessor"));

        add(def("demo-03-drone-mavlink-guard", "demo03", "Drone MAVLink mission safety guard", "SIM-ONLY", 50,
                List.of("BatterySignal", "GpsSignal", "AltitudeSignal", "GeofenceSignal", "MissionCommandSignal", "MissionGuardSignal"),
                List.of("MissionGuardSignal", "MissionGuardSignal", "MissionGuardSignal", "MissionGuardSignal"),
                List.of("risk-feature", "mission-guard", "command-veto", "result-conversion"),
                List.of(5, 3, 2, 2),
                "DroneMissionDemoInput", "DroneMissionNeuron", "DroneMissionResultNeuron", "DroneMissionProcessor"));

        add(def("demo-04-clinical-fhir-advisory", "demo04", "Clinical FHIR advisory", "ADVISORY", 80,
                List.of("VitalSignal", "MedicationContextSignal", "ClinicalRiskSignal", "ClinicalAdvisorySignal"),
                List.of("ClinicalRiskSignal", "ClinicalAdvisorySignal", "ClinicalAdvisorySignal", "ClinicalAdvisorySignal"),
                List.of("vitals-feature", "clinical-risk", "clinician-review", "result-conversion"),
                List.of(4, 3, 2, 2),
                "ClinicalFhirDemoInput", "ClinicalFhirNeuron", "ClinicalFhirResultNeuron", "ClinicalFhirProcessor"));

        add(def("demo-05-dicom-readonly-context", "demo05", "DICOM read-only context bridge", "READ-ONLY", 60,
                List.of("DicomMetadataSignal", "ImageQcSignal", "RoutingAdvisorySignal"),
                List.of("ImageQcSignal", "RoutingAdvisorySignal", "RoutingAdvisorySignal"),
                List.of("context-feature", "routing-qc", "result-conversion"),
                List.of(3, 2, 2),
                "DicomContextDemoInput", "DicomContextNeuron", "DicomContextResultNeuron", "DicomContextProcessor"));

        add(def("demo-06-cybersecurity-kafka-triage", "demo06", "Temporal cybersecurity threat correlation", "ADVISORY", 120,
                List.of("AuthenticationEventSignal", "ProcessEventSignal", "DnsLookupSignal", "NetworkFlowSignal",
                        "ThreatIntelContextSignal", "AssetContextSignal", "MaintenanceWindowSignal"),
                List.of("RiskScoreSignal", "ThreatHypothesisSignal", "SecurityAdvisorySignal", "SecurityAdvisorySignal"),
                List.of("telemetry-normalize", "temporal-correlation", "investigation-action", "result-conversion"),
                List.of(7, 4, 3, 2),
                "SecurityTriageDemoInput", "SecurityTriageNeuron", "SecurityTriageResultNeuron", "SecurityTriageProcessor"));

        add(def("demo-07-observability-otel-export", "demo07", "OpenTelemetry export-only anomaly summarizer", "EXPORT-ONLY", 90,
                List.of("MetricSignal", "TraceSignal", "LogEventSignal", "ObservabilityAnomalySignal"),
                List.of("ObservabilityAnomalySignal", "ObservabilityAnomalySignal", "ObservabilityAnomalySignal"),
                List.of("anomaly-feature", "root-cause", "result-conversion"),
                List.of(4, 3, 2),
                "ObservabilityOtelDemoInput", "ObservabilityOtelNeuron", "ObservabilityOtelResultNeuron", "ObservabilityOtelProcessor"));

        add(def("demo-08-adaptive-tutoring-lti", "demo08", "Adaptive tutoring LTI xAPI advisory", "ADVISORY", 100,
                List.of("LearnerEventSignal", "MasterySignal", "AffectSignal", "TutorAdvisorySignal"),
                List.of("MasterySignal", "AffectSignal", "TutorAdvisorySignal", "TutorAdvisorySignal"),
                List.of("learner-feature", "affect-estimate", "tutor-policy", "result-conversion"),
                List.of(4, 3, 2, 2),
                "AdaptiveTutoringDemoInput", "AdaptiveTutoringNeuron", "AdaptiveTutoringResultNeuron", "AdaptiveTutoringProcessor"));

        add(def("demo-09-nengo-interop", "demo09", "Nengo interoperability vector stream", "ADVISORY", 75,
                List.of("NengoVectorSignal", "TemporalFeatureSignal", "NengoOutputSignal"),
                List.of("TemporalFeatureSignal", "NengoOutputSignal", "NengoOutputSignal", "NengoOutputSignal"),
                List.of("vector-transform", "temporal-smoothing", "decision-advisory", "result-conversion"),
                List.of(3, 3, 2, 2),
                "NengoInteropDemoInput", "NengoInteropNeuron", "NengoInteropResultNeuron", "NengoInteropProcessor"));
    }

    private DemoCatalog() {
    }

    public static Collection<DemoDefinition> all() {
        return DEMOS.values();
    }

    public static DemoDefinition get(String id) {
        DemoDefinition definition = DEMOS.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown full-run demo id: " + id);
        }
        return definition;
    }

    private static void add(DemoDefinition definition) {
        DEMOS.put(definition.id(), definition);
    }

    private static DemoDefinition def(String id, String packageSuffix, String title, String safetyMode, int defaultTicks,
                                      List<String> signalSimpleNames, List<String> layerOutputSimpleNames,
                                      List<String> layerStages, List<Integer> layerSizes,
                                      String inputSimpleName, String neuronSimpleName,
                                      String resultNeuronSimpleName, String processorSimpleName) {
        String pkg = BASE + "." + packageSuffix;
        return new DemoDefinition(
                id,
                pkg,
                title,
                safetyMode,
                defaultTicks,
                signalSimpleNames.stream().map(name -> pkg + ".signals." + name).toList(),
                layerOutputSimpleNames.stream().map(name -> pkg + ".signals." + name).toList(),
                layerStages,
                layerSizes,
                pkg + ".inputs." + inputSimpleName,
                pkg + ".neurons." + neuronSimpleName,
                pkg + ".neurons." + resultNeuronSimpleName,
                pkg + ".processors." + processorSimpleName
        );
    }
}
