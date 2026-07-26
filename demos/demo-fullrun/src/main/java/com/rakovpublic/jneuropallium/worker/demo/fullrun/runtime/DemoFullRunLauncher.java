package com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rakovpublic.jneuropallium.worker.net.signals.OneToAllFirstLayerInputStrategy;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public final class DemoFullRunLauncher {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ENTRY_CLASS = "com.rakovpublic.jneuropallium.worker.application.Entry";
    private static final String CONTEXT_CLASS = DemoJsonContext.class.getName();

    private DemoFullRunLauncher() {
    }

    public static void main(String[] args) throws Exception {
        Arguments arguments = Arguments.parse(args);
        List<DemoRunManifest> manifests;
        if ("all".equals(arguments.demoId)) {
            manifests = runAll(arguments.outputDir, arguments.ticksOverride);
        } else {
            manifests = List.of(runOne(DemoCatalog.get(arguments.demoId), arguments.outputDir, arguments.ticksOverride));
            writeSummary(arguments.outputDir, manifests);
        }
        boolean failed = manifests.stream().anyMatch(manifest -> !"PASS".equals(manifest.status));
        if (failed) {
            throw new IllegalStateException("One or more full-run demos failed; see " + arguments.outputDir.resolve("summary.json"));
        }
    }

    public static List<DemoRunManifest> runAll(Path outputDir, Integer ticksOverride) throws Exception {
        recreateDirectory(outputDir);
        List<DemoRunManifest> manifests = new ArrayList<>();
        for (DemoDefinition definition : DemoCatalog.all()) {
            manifests.add(runOne(definition, outputDir, ticksOverride));
        }
        writeSummary(outputDir, manifests);
        return manifests;
    }

    public static DemoRunManifest runOne(DemoDefinition definition, Path rootOutputDir, Integer ticksOverride) throws Exception {
        int ticks = ticksOverride == null ? definition.defaultTicks() : ticksOverride;
        Path demoDir = rootOutputDir.resolve(definition.id());
        recreateDirectory(demoDir);
        Path layersDir = demoDir.resolve("layers");
        Files.createDirectories(layersDir);
        Path outputPath = demoDir.resolve("results.jsonl");
        Path auditPath = demoDir.resolve("audit.jsonl");
        Path contextPath = demoDir.resolve("context.json");
        Path entryLogPath = demoDir.resolve("entry.log");
        Path modelJar = demoDir.resolve("demo-model.jar");

        buildModelJar(modelJar);
        writeLayerMetadata(definition, layersDir);
        String contextJson = writeContext(definition, ticks, demoDir, layersDir, outputPath, auditPath, contextPath, modelJar);

        DemoRunManifest manifest = new DemoRunManifest();
        manifest.demoId = definition.id();
        manifest.mode = "local";
        manifest.modelJarPath = modelJar.toAbsolutePath().toString();
        manifest.contextClass = CONTEXT_CLASS;
        manifest.contextJsonPath = contextPath.toAbsolutePath().toString();
        manifest.layerMetaPath = layersDir.toAbsolutePath().toString();
        manifest.outputPath = outputPath.toAbsolutePath().toString();
        manifest.auditPath = auditPath.toAbsolutePath().toString();
        manifest.entryLogPath = entryLogPath.toAbsolutePath().toString();
        manifest.ticks = ticks;

        manifest.exitCode = runEntryProcess(modelJar, contextJson, entryLogPath);
        applyValidation(definition, outputPath, manifest);
        manifest.status = manifest.exitCode == 0 && manifest.behaviorAssertions.values().stream().allMatch(Boolean::booleanValue)
                ? "PASS" : "FAIL";
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(demoDir.resolve("manifest.json").toFile(), manifest);
        return manifest;
    }

    private static int runEntryProcess(Path modelJar, String contextJson, Path entryLogPath) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(javaBinary());
        command.add("-Xms16m");
        command.add("-Xmx128m");
        command.add("-XX:+UseSerialGC");
        command.add("-XX:MaxMetaspaceSize=96m");
        command.add("-XX:ReservedCodeCacheSize=32m");
        command.add("-Xss256k");
        command.add("-Xint");
        command.add(ENTRY_CLASS);
        command.add("local");
        command.add(modelJar.toUri().toURL().toString());
        command.add(CONTEXT_CLASS);
        command.add(contextJson);

        Files.writeString(entryLogPath, String.join(System.lineSeparator(), command)
                + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.environment().put("CLASSPATH", launcherClasspath());
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(entryLogPath.toFile()));
        Process process = processBuilder.start();
        return process.waitFor();
    }

    private static String launcherClasspath() {
        String surefireClasspath = System.getProperty("surefire.test.class.path");
        if (surefireClasspath != null && !surefireClasspath.isBlank()) {
            return surefireClasspath;
        }
        return System.getProperty("java.class.path");
    }

    private static String javaBinary() {
        Path javaHome = Path.of(System.getProperty("java.home"));
        String executable = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "java.exe" : "java";
        return javaHome.resolve("bin").resolve(executable).toString();
    }

    private static void writeLayerMetadata(DemoDefinition definition, Path layersDir) throws IOException {
        for (int i = 0; i < definition.layerSizes().size(); i++) {
            ObjectNode layer = MAPPER.createObjectNode();
            int layerSize = definition.layerSizes().get(i);
            layer.put("layerID", i);
            layer.put("layerSize", layerSize);
            ArrayNode neurons = layer.putArray("neurons");
            for (int neuronId = 0; neuronId < layerSize; neuronId++) {
                neurons.add(neuronJson(definition, i, neuronId));
            }
            layer.set("metaParams", MAPPER.createObjectNode());
            Files.writeString(layersDir.resolve(String.valueOf(i)), MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(layer),
                    StandardCharsets.UTF_8);
        }
    }

    private static ObjectNode neuronJson(DemoDefinition definition, int layerIndex, int neuronId) {
        ObjectNode neuron = MAPPER.createObjectNode();
        boolean resultLayer = definition.isResultLayer(layerIndex);
        String neuronClass = resultLayer ? definition.resultNeuronClassName() : definition.neuronClassName();
        String outputSignal = definition.outputSignalClass(layerIndex);
        List<String> acceptedSignals = definition.acceptedSignalClasses(layerIndex);
        neuron.put("neuronId", neuronId);
        neuron.put("currentNeuronClass", neuronClass);
        neuron.put("demoId", definition.id());
        neuron.put("layerRole", definition.layerStages().get(layerIndex));
        neuron.put("neuronLabel", definition.layerStages().get(layerIndex) + "-" + neuronId);
        neuron.put("isProcessed", false);
        neuron.put("changed", false);
        neuron.put("onDelete", false);
        neuron.put("run", -1);

        ObjectNode processorMap = neuron.putObject("processorMap");
        for (String acceptedSignal : acceptedSignals) {
            ObjectNode processor = processorMap.putObject(acceptedSignal);
            processor.put("signalProcessorClass", definition.processorClassName());
            processor.put("signalClassName", acceptedSignal);
            processor.put("outputSignalClassName", outputSignal);
            processor.put("stage", definition.layerStages().get(layerIndex));
            processor.put("description", definition.title() + " " + definition.layerStages().get(layerIndex));
        }
        neuron.set("mergerMap", MAPPER.createObjectNode());
        neuron.set("activationFunctions", MAPPER.createObjectNode());
        neuron.set("axon", axonJson(definition, layerIndex, neuronId, outputSignal));
        ObjectNode chain = neuron.putObject("signalChain");
        chain.put("signalChainClass", DemoSignalChain.class.getName());
        ArrayNode signalClassNames = chain.putArray("signalClassNames");
        acceptedSignals.forEach(signalClassNames::add);
        chain.put("description", definition.title() + " layer " + layerIndex + " chain");
        return neuron;
    }

    private static ObjectNode axonJson(DemoDefinition definition, int layerIndex, int neuronId, String outputSignal) {
        ObjectNode axon = MAPPER.createObjectNode();
        ObjectNode connectionMap = axon.putObject("connectionMap");
        ObjectNode addressMap = axon.putObject("addressMap");
        ObjectNode defaultWeights = axon.putObject("defaultWeights");
        axon.put("connectionsWrapped", false);
        ObjectNode defaultWeight = defaultWeights.putObject(outputSignal);
        defaultWeight.put("weightClass", DemoPassThroughWeight.class.getName());
        defaultWeight.put("signalClassName", outputSignal);
        if (!definition.isResultLayer(layerIndex)) {
            int targetLayer = layerIndex + 1;
            int targetSize = definition.layerSizes().get(targetLayer);
            ArrayNode connections = connectionMap.putArray(outputSignal);
            ObjectNode layerAddress = addressMap.putObject(String.valueOf(targetLayer));
            for (int offset = 0; offset < Math.min(2, targetSize); offset++) {
                int targetNeuron = (neuronId + offset) % targetSize;
                ObjectNode connection = connections.addObject();
                connection.put("targetLayerId", targetLayer);
                connection.put("sourceLayerId", layerIndex);
                connection.put("targetNeuronId", targetNeuron);
                connection.put("sourceNeuronId", neuronId);
                ObjectNode weight = connection.putObject("weight");
                weight.put("weightClass", DemoPassThroughWeight.class.getName());
                weight.put("signalClassName", outputSignal);
                connection.put("description", definition.id() + " " + layerIndex + "->" + targetLayer);
                layerAddress.putArray(String.valueOf(targetNeuron));
            }
        }
        return axon;
    }

    private static String writeContext(DemoDefinition definition, int ticks, Path demoDir, Path layersDir,
                                       Path outputPath, Path auditPath, Path contextPath, Path modelJar) throws IOException {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("configuration.input.layermeta", layersDir.toAbsolutePath().toString());
        properties.put("configuration.neuronnet.classes", String.join(",", definition.modelClasses()));
        properties.put("configuration.storage.json", storageJson(demoDir));
        properties.put("configuration.history.slow.runs", "2");
        properties.put("configuration.history.fast.runs", "2");
        properties.put("configuration.slowfast.ratio", "1");
        properties.put("configuration.processing.frequency.map", frequencyMap(definition));
        properties.put("configuration.input.inputs", inputsJson(definition, ticks));
        properties.put("configuration.isteacherstudying", "true");
        properties.put("configuration.maxRun", String.valueOf(ticks));
        properties.put("configuration.infiniteRun", "false");
        properties.put("configuration.outputAggregator", JsonlResultAggregator.class.getName());
        properties.put("worker.threads.amount", "1");
        properties.put("configuration.discriminatorsAmount", "0");
        properties.put("configuration.demo.id", definition.id());
        properties.put("configuration.demo.title", definition.title());
        properties.put("configuration.demo.safetyMode", definition.safetyMode());
        properties.put("configuration.demo.output.path", outputPath.toAbsolutePath().toString());
        properties.put("configuration.demo.audit.path", auditPath.toAbsolutePath().toString());
        properties.put("configuration.demo.entry.mode", "local");
        properties.put("configuration.demo.modelJar.path", modelJar.toAbsolutePath().toString());
        properties.put("configuration.demo.deterministicTimestamp", "true");
        properties.put("configuration.demo.timestampBase", "1700000000000");
        ObjectNode root = MAPPER.createObjectNode();
        ObjectNode props = root.putObject("properties");
        properties.forEach(props::put);
        String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        Files.writeString(contextPath, json, StandardCharsets.UTF_8);
        return contextPath.toAbsolutePath().toString();
    }

    private static String storageJson(Path demoDir) throws IOException {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("storageClass", DemoFileStorage.class.getName());
        ObjectNode storage = root.putObject("storage");
        storage.put("rootPath", demoDir.toAbsolutePath().toString());
        return MAPPER.writeValueAsString(root);
    }

    private static String frequencyMap(DemoDefinition definition) throws IOException {
        ObjectNode root = MAPPER.createObjectNode();
        for (String signalClass : definition.allSignalClasses()) {
            ObjectNode frequency = root.putObject(signalClass);
            frequency.put("epoch", "1");
            frequency.put("loop", "1");
        }
        return MAPPER.writeValueAsString(root);
    }

    private static String inputsJson(DemoDefinition definition, int ticks) throws IOException {
        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode inputData = root.putArray("inputData");
        ObjectNode item = inputData.addObject();
        ObjectNode source = item.putObject("iInputSource");
        source.put("clazz", definition.inputClassName());
        ObjectNode initInput = source.putObject("initInput");
        initInput.put("demoId", definition.id());
        initInput.put("name", definition.id() + "-input");
        initInput.put("ticks", ticks);
        initInput.put("cursor", 0);
        initInput.put("epoch", 1);
        initInput.put("loop", 1);
        item.put("mandatory", true);
        ObjectNode strategy = item.putObject("initStrategy");
        strategy.put("clazz", OneToAllFirstLayerInputStrategy.class.getName());
        strategy.putObject("iNeuronNetInput");
        item.put("amountOfRuns", ticks);
        return MAPPER.writeValueAsString(root);
    }

    private static void buildModelJar(Path modelJar) throws IOException, URISyntaxException {
        CodeSource codeSource = DemoFullRunLauncher.class.getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            throw new IllegalStateException("Cannot resolve worker code source for demo model jar");
        }
        Path source = Path.of(codeSource.getLocation().toURI());
        Files.createDirectories(modelJar.getParent());
        if (Files.isRegularFile(source) && source.toString().endsWith(".jar")) {
            Files.copy(source, modelJar, StandardCopyOption.REPLACE_EXISTING);
        } else if (Files.isDirectory(source)) {
            jarDirectory(source, modelJar);
        } else {
            throw new IllegalStateException("Unsupported worker code source for model jar: " + source);
        }
    }

    private static void jarDirectory(Path classesDir, Path jarPath) throws IOException {
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(jarPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
            try (var stream = Files.walk(classesDir)) {
                for (Path file : stream.filter(Files::isRegularFile).sorted().toList()) {
                    String entryName = classesDir.relativize(file).toString().replace('\\', '/');
                    jar.putNextEntry(new JarEntry(entryName));
                    try (InputStream input = Files.newInputStream(file)) {
                        input.transferTo(jar);
                    }
                    jar.closeEntry();
                }
            }
        }
    }

    private static void applyValidation(DemoDefinition definition, Path outputPath, DemoRunManifest manifest) throws IOException {
        List<JsonNode> lines = readJsonLines(outputPath);
        manifest.outputRows = lines.size();
        manifest.metrics.put("resultLines", lines.size());
        manifest.behaviorAssertions.put("modeLocal", "local".equals(manifest.mode));
        manifest.behaviorAssertions.put("outputJsonlExists", Files.exists(outputPath));
        manifest.behaviorAssertions.put("aggregatorCalled", !lines.isEmpty());
        switch (definition.id()) {
            case "demo-01-industrial-control" -> validateIndustrial(lines, manifest);
            case "demo-02-pump-fleet-maintenance" -> validatePump(lines, manifest);
            case "demo-03-drone-mavlink-guard" -> validateDrone(lines, manifest);
            case "demo-04-clinical-fhir-advisory" -> validateClinical(lines, manifest);
            case "demo-05-dicom-readonly-context" -> validateDicom(lines, manifest);
            case "demo-06-cybersecurity-kafka-triage" -> validateSecurity(lines, manifest);
            case "demo-07-observability-otel-export" -> validateObservability(lines, manifest);
            case "demo-08-adaptive-tutoring-lti" -> validateTutoring(lines, manifest);
            case "demo-09-nengo-interop" -> validateNengo(lines, manifest);
            default -> throw new IllegalArgumentException("Unknown demo id " + definition.id());
        }
    }

    private static List<JsonNode> readJsonLines(Path outputPath) throws IOException {
        if (!Files.exists(outputPath)) {
            return List.of();
        }
        List<JsonNode> result = new ArrayList<>();
        for (String line : Files.readAllLines(outputPath, StandardCharsets.UTF_8)) {
            if (!line.isBlank()) {
                result.add(MAPPER.readTree(line));
            }
        }
        return result;
    }

    private static void validateIndustrial(List<JsonNode> lines, DemoRunManifest manifest) {
        List<JsonNode> results = flatten(lines);
        manifest.behaviorAssertions.put("atLeast100ResultRows", results.size() >= Math.min(100, manifest.ticks));
        manifest.behaviorAssertions.put("normalValveCommand", any(results, "resultType", "VALVE_COMMAND"));
        manifest.behaviorAssertions.put("forcedAlarmFailSafeWithinOneTick", anyTick(results, "FAIL_SAFE_COMMAND", 5, 6));
        manifest.behaviorAssertions.put("manualOverrideHeld", any(results, "resultType", "HELD_COMMAND"));
    }

    private static void validatePump(List<JsonNode> lines, DemoRunManifest manifest) {
        List<JsonNode> results = flatten(lines);
        double degrading = minValue(results, "pump-03", Double.MAX_VALUE);
        double healthy = minValue(results, "pump-00", Double.MAX_VALUE);
        long healthyAdvisories = count(results, "pump-00", "MAINTENANCE_ADVISORY");
        manifest.metrics.put("pump03MinRul", degrading);
        manifest.metrics.put("pump00MinRul", healthy);
        manifest.behaviorAssertions.put("degradingPumpLowerRul", degrading < healthy);
        manifest.behaviorAssertions.put("maintenanceAdvisoryEmitted", count(results, "pump-03", "MAINTENANCE_ADVISORY") > 0);
        manifest.behaviorAssertions.put("offlinePumpAdvisory", count(results, "pump-07", "DEVICE_OFFLINE_ADVISORY") > 0);
        manifest.behaviorAssertions.put("healthyPumpZeroMaintenanceAdvisory", healthyAdvisories == 0);
    }

    private static void validateDrone(List<JsonNode> lines, DemoRunManifest manifest) {
        List<JsonNode> results = flatten(lines);
        manifest.metrics.put("allowedCount", countType(results, "COMMAND_ALLOWED"));
        manifest.metrics.put("vetoCount", countType(results, "COMMAND_VETO"));
        manifest.behaviorAssertions.put("commandAllowed", countType(results, "COMMAND_ALLOWED") > 0);
        manifest.behaviorAssertions.put("commandVetoed", countType(results, "COMMAND_VETO") > 0);
        manifest.behaviorAssertions.put("returnToHomeAdvisory", countType(results, "RETURN_TO_HOME_ADVISORY") > 0);
    }

    private static void validateClinical(List<JsonNode> lines, DemoRunManifest manifest) {
        List<JsonNode> results = flatten(lines);
        manifest.behaviorAssertions.put("advisoryMode", allMode(results, "ADVISORY"));
        manifest.behaviorAssertions.put("noAutonomousOrderOrWrite", noResultTypeContains(results, "ORDER", "WRITE"));
        manifest.behaviorAssertions.put("highRiskPatientAdvisory", count(results, "patient-high-risk", "CLINICIAN_REVIEW_ADVISORY") > 0);
    }

    private static void validateDicom(List<JsonNode> lines, DemoRunManifest manifest) {
        List<JsonNode> results = flatten(lines);
        manifest.behaviorAssertions.put("readOnlyMode", allMode(results, "READ-ONLY"));
        manifest.behaviorAssertions.put("missingMetadataQcAdvisory", count(results, "study-missing-metadata", "QC_ADVISORY") > 0);
        manifest.behaviorAssertions.put("noCommandOrWriteback", noResultTypeContains(results, "COMMAND", "WRITE"));
    }

    private static void validateSecurity(List<JsonNode> lines, DemoRunManifest manifest) {
        List<JsonNode> results = flatten(lines);
        double attack = maxAttribute(results, "user:backup-service@workstation-17", "posterior");
        double benign = maxAttribute(results, "svc:deployment-agent@web-tier", "posterior");
        double slow = maxAttribute(results, "host:finance-file-01", "posterior");
        manifest.metrics.put("attackScore", attack);
        manifest.metrics.put("benignScore", benign);
        manifest.metrics.put("slowExfiltrationScore", slow);
        manifest.behaviorAssertions.put("attackScoreGreaterThanBenign", attack > benign);
        manifest.behaviorAssertions.put("temporalAttackChainDetected",
                count(results, "user:backup-service@workstation-17", "TEMPORAL_THREAT_ADVISORY") > 0);
        manifest.behaviorAssertions.put("maintenanceContextSuppressed",
                count(results, "svc:deployment-agent@web-tier", "CONTEXT_SUPPRESSED_OBSERVATION") > 0);
        manifest.behaviorAssertions.put("lowAndSlowCorrelation",
                count(results, "host:finance-file-01", "LOW_AND_SLOW_CORRELATION") > 0);
        manifest.behaviorAssertions.put("baselineFrozenDuringAttack", results.stream().anyMatch(node ->
                "user:backup-service@workstation-17".equals(node.path("entityId").asText())
                        && node.path("attributes").path("baselineFrozen").asBoolean(false)));
        manifest.behaviorAssertions.put("allTrainingSourcesReferenced", results.stream().anyMatch(node ->
                node.path("attributes").path("trainingSources").asText("").contains("LANL")
                        && node.path("attributes").path("trainingSources").asText("").contains("ToN_IoT")
                        && node.path("attributes").path("trainingSources").asText("").contains("OpTC")
                        && node.path("attributes").path("trainingSources").asText("").contains("CIC-IDS2017")
                        && node.path("attributes").path("trainingSources").asText("").contains("UNSW-NB15")
                        && node.path("attributes").path("trainingSources").asText("").contains("CALDERA")));
        manifest.behaviorAssertions.put("advisoryOnly", noResultTypeContains(results, "BLOCK"));
    }

    private static void validateObservability(List<JsonNode> lines, DemoRunManifest manifest) {
        List<JsonNode> results = flatten(lines);
        manifest.behaviorAssertions.put("exportOnlyMode", allMode(results, "EXPORT-ONLY"));
        manifest.behaviorAssertions.put("noWritebackControl", noResultTypeContains(results, "WRITE", "CONTROL"));
        manifest.behaviorAssertions.put("anomalyWindowReported", results.stream().anyMatch(node ->
                node.path("attributes").has("anomalyWindowStart") && node.path("attributes").has("anomalyWindowEnd")));
    }

    private static void validateTutoring(List<JsonNode> lines, DemoRunManifest manifest) {
        List<JsonNode> results = flatten(lines);
        manifest.behaviorAssertions.put("strugglingLearnerHint", anyDecision(results, "learner-struggling", "RECOMMEND_HINT_AND_LOWER_DIFFICULTY"));
        manifest.behaviorAssertions.put("strongLearnerHarderExercise", anyDecision(results, "learner-strong", "RECOMMEND_HARDER_EXERCISE"));
    }

    private static void validateNengo(List<JsonNode> lines, DemoRunManifest manifest) {
        List<JsonNode> results = flatten(lines);
        manifest.metrics.put("nengoResultRows", results.size());
        manifest.behaviorAssertions.put("expectedVectorRows", results.size() >= manifest.ticks);
        manifest.behaviorAssertions.put("confidenceEmitted", results.stream().anyMatch(node -> node.path("confidence").asDouble(0.0) > 0.0));
    }

    private static List<JsonNode> flatten(List<JsonNode> lines) {
        List<JsonNode> result = new ArrayList<>();
        for (JsonNode line : lines) {
            line.path("results").forEach(result::add);
        }
        return result;
    }

    private static boolean any(List<JsonNode> nodes, String field, String value) {
        return nodes.stream().anyMatch(node -> value.equals(node.path(field).asText()));
    }

    private static boolean anyTick(List<JsonNode> nodes, String resultType, int minTick, int maxTick) {
        return nodes.stream().anyMatch(node -> resultType.equals(node.path("resultType").asText())
                && node.path("tick").asInt(-1) >= minTick && node.path("tick").asInt(-1) <= maxTick);
    }

    private static boolean anyDecision(List<JsonNode> nodes, String entityId, String decision) {
        return nodes.stream().anyMatch(node -> entityId.equals(node.path("entityId").asText())
                && decision.equals(node.path("decision").asText()));
    }

    private static long count(List<JsonNode> nodes, String entityId, String resultType) {
        return nodes.stream().filter(node -> entityId.equals(node.path("entityId").asText())
                && resultType.equals(node.path("resultType").asText())).count();
    }

    private static long countType(List<JsonNode> nodes, String resultType) {
        return nodes.stream().filter(node -> resultType.equals(node.path("resultType").asText())).count();
    }

    private static double minValue(List<JsonNode> nodes, String entityId, double fallback) {
        return nodes.stream()
                .filter(node -> entityId.equals(node.path("entityId").asText()))
                .mapToDouble(node -> node.path("value").asDouble(fallback))
                .min()
                .orElse(fallback);
    }

    private static double maxAttribute(List<JsonNode> nodes, String entityId, String attribute) {
        return nodes.stream()
                .filter(node -> entityId.equals(node.path("entityId").asText()))
                .mapToDouble(node -> node.path("attributes").path(attribute).asDouble(0.0))
                .max()
                .orElse(0.0);
    }

    private static boolean allMode(List<JsonNode> nodes, String mode) {
        return !nodes.isEmpty() && nodes.stream().allMatch(node -> mode.equals(node.path("mode").asText()));
    }

    private static boolean noResultTypeContains(List<JsonNode> nodes, String... forbidden) {
        return nodes.stream().noneMatch(node -> {
            String type = node.path("resultType").asText("").toUpperCase(Locale.ROOT);
            String decision = node.path("decision").asText("").toUpperCase(Locale.ROOT);
            for (String word : forbidden) {
                if (type.contains(word) || decision.contains(word)) {
                    return true;
                }
            }
            return false;
        });
    }

    private static void writeSummary(Path outputDir, List<DemoRunManifest> manifests) throws IOException {
        Files.createDirectories(outputDir);
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(outputDir.resolve("summary.json").toFile(), manifests);
    }

    private static void recreateDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (var stream = Files.walk(dir)) {
                stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            } catch (UncheckedIOException e) {
                throw e.getCause();
            }
        }
        Files.createDirectories(dir);
    }

    private record Arguments(String demoId, Path outputDir, Integer ticksOverride) {
        private static Arguments parse(String[] args) {
            String demoId = args.length == 0 ? "all" : args[0];
            Path output = Path.of("target", "jneopallium-fullrun-demos");
            Integer ticks = null;
            for (int i = 1; i < args.length; i++) {
                if ("--output".equals(args[i]) && i + 1 < args.length) {
                    output = Path.of(args[++i]);
                } else if ("--ticks".equals(args[i]) && i + 1 < args.length) {
                    ticks = Integer.parseInt(args[++i]);
                }
            }
            return new Arguments(demoId, output, ticks);
        }
    }
}
