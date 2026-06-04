package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public final class AutonomousMindLayerMetaGenerator {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SIGNAL_CLASS = AutonomousMindSignal.class.getName();
    public static final List<String> LAYER_NAMES = List.of(
            "System 0 - Sensor gateway",
            "System 1 - Modality perception",
            "System 2 - Sensor fusion and world state",
            "System 3 - Attention and task relevance",
            "System 4 - Working memory / global workspace",
            "System 5 - Owner task manager",
            "System 6 - Memory",
            "System 7 - Prediction and imagination",
            "System 8 - Planning",
            "System 9 - Safety / harm / permission gate",
            "System 10 - Action selection and execution",
            "System 11 - Learning / investigation / sleep optimizer",
            "Result - Transparent cognitive run output");
    private static final List<List<String>> NEURON_LABELS = List.of(
            List.of("SensorGatewayNeuron", "OwnerCommandInputNeuron", "SelfDiagnosticsInputNeuron",
                    "EnergyInputNeuron", "ClockInputNeuron"),
            List.of("VisualFeatureNeuron", "LidarFeatureNeuron", "DepthFeatureNeuron", "IRFeatureNeuron",
                    "ThermalFeatureNeuron", "UVFeatureNeuron", "RadiationFeatureNeuron", "RadioFeatureNeuron",
                    "RadarFeatureNeuron", "SoundFeatureNeuron", "UltrasoundFeatureNeuron",
                    "MagneticFeatureNeuron", "ChemicalFeatureNeuron", "NetworkFeatureNeuron"),
            List.of("ObjectFusionNeuron", "SpatialMapNeuron", "SourceConfidenceNeuron",
                    "SensorContradictionDetectorNeuron", "WorldStateNeuron", "AnomalyNeuron"),
            List.of("TaskRelevanceNeuron", "SafetyRelevanceNeuron", "NoveltyNeuron", "UncertaintyNeuron",
                    "AttentionGateNeuron"),
            List.of("WorkingMemoryWriteNeuron", "WorkingMemoryReadNeuron", "GlobalWorkspaceNeuron",
                    "ContextBindingNeuron", "TaskStateNeuron"),
            List.of("OwnerTaskParserNeuron", "TaskDecomposerNeuron", "TaskPriorityNeuron",
                    "TaskConstraintNeuron", "TaskProgressNeuron", "TaskCompletionNeuron",
                    "TaskPauseResumeNeuron", "OwnerClarificationNeuron"),
            List.of("EpisodicMemoryNeuron", "SemanticMemoryNeuron", "ProceduralSkillNeuron",
                    "MapMemoryNeuron", "TaskTemplateMemoryNeuron", "FailureMemoryNeuron",
                    "CalibrationMemoryNeuron", "MemoryConsolidationNeuron"),
            List.of("TransitionPredictionNeuron", "SensorPredictionNeuron", "TaskOutcomePredictionNeuron",
                    "EnergyPredictionNeuron", "RiskPredictionNeuron", "CounterfactualSimulatorNeuron"),
            List.of("SubgoalPlannerNeuron", "RoutePlannerNeuron", "ScanPatternPlannerNeuron",
                    "EnergyAwarePlannerNeuron", "InvestigationPlannerNeuron", "FallbackPlannerNeuron"),
            List.of("PermissionGateNeuron", "ConsequenceModelNeuron", "HarmEvaluationNeuron",
                    "HardConstraintNeuron", "SafeAlternativeNeuron", "OwnerClarificationNeuron",
                    "TransparencyNeuron"),
            List.of("CompetitiveActionSelectionNeuron", "MotorCommandNeuron", "ScanCommandNeuron",
                    "ReportCommandNeuron", "DockingCommandNeuron", "SleepModeCommandNeuron"),
            List.of("IdleLearningNeuron", "FreeInvestigationNeuron", "ModelOptimizationNeuron",
                    "SleepConsolidationNeuron", "SelfTestNeuron", "SensorCalibrationNeuron",
                    "SkillRefinementNeuron", "ContradictionResolutionNeuron"),
            List.of("AutonomousMindResultNeuron"));

    private AutonomousMindLayerMetaGenerator() {
    }

    public static void writeLayerMetadata(Path layersDir) throws IOException {
        Files.createDirectories(layersDir);
        for (int layer = 0; layer < NEURON_LABELS.size(); layer++) {
            ObjectNode layerJson = MAPPER.createObjectNode();
            layerJson.put("layerID", layer);
            layerJson.put("layerSize", NEURON_LABELS.get(layer).size());
            ArrayNode neurons = layerJson.putArray("neurons");
            for (int neuronId = 0; neuronId < NEURON_LABELS.get(layer).size(); neuronId++) {
                neurons.add(neuronJson(layer, neuronId));
            }
            layerJson.set("metaParams", MAPPER.createObjectNode());
            Files.writeString(layersDir.resolve(String.valueOf(layer)),
                    MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(layerJson),
                    StandardCharsets.UTF_8);
        }
    }

    private static ObjectNode neuronJson(int layer, int neuronId) {
        ObjectNode neuron = MAPPER.createObjectNode();
        boolean resultLayer = layer == NEURON_LABELS.size() - 1;
        neuron.put("neuronId", neuronId);
        neuron.put("currentNeuronClass", resultLayer ? AutonomousMindResultNeuron.class.getName()
                : AutonomousMindNeuron.class.getName());
        neuron.put("cognitiveSystem", LAYER_NAMES.get(layer));
        neuron.put("layerRole", LAYER_NAMES.get(layer));
        neuron.put("layerName", LAYER_NAMES.get(layer));
        neuron.put("neuronLabel", NEURON_LABELS.get(layer).get(neuronId));
        neuron.put("isProcessed", false);
        neuron.put("changed", false);
        neuron.put("onDelete", false);
        neuron.put("run", -1);
        ObjectNode processorMap = neuron.putObject("processorMap");
        ObjectNode processor = processorMap.putObject(SIGNAL_CLASS);
        processor.put("signalProcessorClass", AutonomousMindSignalProcessor.class.getName());
        processor.put("signalClassName", SIGNAL_CLASS);
        processor.put("outputSignalClassName", SIGNAL_CLASS);
        processor.put("stage", stageSlug(layer));
        processor.put("layerName", LAYER_NAMES.get(layer));
        processor.put("description", LAYER_NAMES.get(layer));
        neuron.set("mergerMap", MAPPER.createObjectNode());
        neuron.set("activationFunctions", MAPPER.createObjectNode());
        neuron.set("axon", axonJson(layer, neuronId));
        ObjectNode chain = neuron.putObject("signalChain");
        chain.put("signalChainClass", AutonomousMindSignalChain.class.getName());
        chain.putArray("signalClassNames").add(SIGNAL_CLASS);
        chain.put("description", LAYER_NAMES.get(layer) + " chain");
        return neuron;
    }

    private static ObjectNode axonJson(int layer, int neuronId) {
        ObjectNode axon = MAPPER.createObjectNode();
        ObjectNode connectionMap = axon.putObject("connectionMap");
        ObjectNode addressMap = axon.putObject("addressMap");
        ObjectNode defaultWeights = axon.putObject("defaultWeights");
        axon.put("connectionsWrapped", false);
        ObjectNode defaultWeight = defaultWeights.putObject(SIGNAL_CLASS);
        defaultWeight.put("weightClass", AutonomousMindPassThroughWeight.class.getName());
        defaultWeight.put("signalClassName", SIGNAL_CLASS);
        if (layer < NEURON_LABELS.size() - 1) {
            int targetLayer = layer + 1;
            int targetNeuron = neuronId % NEURON_LABELS.get(targetLayer).size();
            ArrayNode connections = connectionMap.putArray(SIGNAL_CLASS);
            ObjectNode connection = connections.addObject();
            connection.put("targetLayerId", targetLayer);
            connection.put("sourceLayerId", layer);
            connection.put("targetNeuronId", targetNeuron);
            connection.put("sourceNeuronId", neuronId);
            ObjectNode weight = connection.putObject("weight");
            weight.put("weightClass", AutonomousMindPassThroughWeight.class.getName());
            weight.put("signalClassName", SIGNAL_CLASS);
            connection.put("description", LAYER_NAMES.get(layer) + " to " + LAYER_NAMES.get(targetLayer));
            addressMap.putObject(String.valueOf(targetLayer)).putArray(String.valueOf(targetNeuron));
        }
        return axon;
    }

    private static String stageSlug(int layer) {
        return LAYER_NAMES.get(layer).toLowerCase(Locale.ROOT)
                .replace("system " + layer + " - ", "")
                .replace("result - ", "")
                .replace("/", "-")
                .replace(",", "")
                .replace(" ", "-");
    }
}
