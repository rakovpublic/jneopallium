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
            "System 0 - Sensory and body input",
            "System 1 - Feature extraction",
            "System 2 - Attention and salience",
            "System 3 - Working memory / global workspace",
            "System 4 - World model and prediction",
            "System 5 - Self model and drives",
            "System 6 - Memory",
            "System 7 - Emotion, neuromodulation, and homeostasis",
            "System 8 - Imagination and planning",
            "System 9 - Social model / theory of mind",
            "System 10 - Harm discriminator / ethics gate",
            "System 11 - Action selection, learning, loop prevention, meta-cognition",
            "Result - Transparent cognitive run output");
    private static final List<List<String>> NEURON_LABELS = List.of(
            List.of("VisualPatchInputNeuron", "AgentPoseInputNeuron", "ObjectInputNeuron",
                    "BystanderInputNeuron", "BodyStateInputNeuron"),
            List.of("FoodFeatureNeuron", "HazardFeatureNeuron", "FragileObjectFeatureNeuron",
                    "BystanderProximityNeuron", "WallFeatureNeuron", "UnknownCellFeatureNeuron"),
            List.of("SalienceNeuron", "AttentionGateNeuron", "NoveltyNeuron", "ThreatPriorityNeuron",
                    "GoalRelevanceNeuron"),
            List.of("WorkingMemoryWriteNeuron", "WorkingMemoryReadNeuron", "GlobalWorkspaceNeuron",
                    "ContextBindingNeuron", "AttentionStabilityNeuron"),
            List.of("TransitionModelNeuron", "RewardPredictionNeuron", "HazardPredictionNeuron",
                    "ObjectDynamicsNeuron", "BystanderPredictionNeuron", "PredictionErrorNeuron"),
            List.of("SelfStateNeuron", "CapabilityNeuron", "ConfidenceNeuron", "AgencyNeuron",
                    "ResponsibilityNeuron", "NeedBalanceNeuron"),
            List.of("EpisodicMemoryNeuron", "SemanticMemoryNeuron", "ProceduralSkillNeuron",
                    "MemoryRecallNeuron", "MemoryConsolidationNeuron", "SleepReplayNeuron"),
            List.of("DopamineNeuron", "SerotoninNeuron", "NorepinephrineNeuron", "AcetylcholineNeuron",
                    "InhibitionNeuron", "StressNeuron", "SocialBondNeuron", "HomeostasisNeuron"),
            List.of("CandidateActionNeuron", "ShortHorizonPlannerNeuron", "CounterfactualSimulatorNeuron",
                    "PlanScoringNeuron", "UncertaintyPenaltyNeuron", "SafePathSearchNeuron"),
            List.of("OtherAgentStateNeuron", "IntentInferenceNeuron", "VulnerabilityNeuron", "TrustNeuron",
                    "SocialNormNeuron", "EmpathyNeuron"),
            List.of("ConsequenceModelNeuron", "HarmEvaluationNeuron", "EthicalPriorityNeuron",
                    "HardConstraintNeuron", "SafeAlternativeNeuron", "HarmGateNeuron", "TransparencyNeuron"),
            List.of("CompetitiveActionSelectionNeuron", "MotorCommandNeuron", "STDPNeuron",
                    "HarmLearningNeuron", "RegionMonitorNeuron", "LoopDetectorNeuron",
                    "LoopCircuitBreakerNeuron", "UncertaintyMonitorNeuron", "ConflictDetectorNeuron",
                    "SelfCritiqueNeuron", "AskForHelpNeuron"),
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
