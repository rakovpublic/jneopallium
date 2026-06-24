package com.rakovpublic.jneuropallium.worker.demo.industrialfmi.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rakovpublic.jneuropallium.worker.application.Entry;
import com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoFileStorage;
import com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoJsonContext;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.AcousticFeatureNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.AdvisoryGateNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.FaultHypothesisNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.IAcousticFeatureNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.IAdvisoryGateNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.IFaultHypothesisNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.IMachineHealthCorrelationNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.IVibrationFeatureNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.MachineHealthCorrelationNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.VibrationFeatureNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.OneToAllFirstLayerInputStrategy;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.FaultHypothesisSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MachineFeatureSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MachineHealthAdvisorySignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MachineWaveformSignal;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial.AcousticFeatureProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial.FaultHypothesisProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial.MachineHealthAdvisoryGateProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial.MachineHealthCorrelationProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial.VibrationFeatureProcessor;

import java.io.IOException;
import java.io.InputStream;
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

public final class IndustrialLoopGuardianEntryLauncher {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ENTRY_CLASS = Entry.class.getName();
    private static final String CONTEXT_CLASS = DemoJsonContext.class.getName();
    private static final String ASSET_ID = "pump-17";
    private static final int FAULT_START_TICK = 20;
    private static final int FAULT_RISE_TICKS = 14;

    private IndustrialLoopGuardianEntryLauncher() {
    }

    public static void main(String[] args) throws Exception {
        Path outputDir = args.length > 0 ? Path.of(args[0]) : Path.of("target", "industrial-loop-guardian-entry");
        int ticks = args.length > 1 ? Integer.parseInt(args[1]) : 80;
        long runOnceInMs = args.length > 2 ? Long.parseLong(args[2]) : 1L;
        IndustrialRunManifest manifest = runPumpWear(outputDir, ticks, runOnceInMs);
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(System.out, manifest);
        if (!"PASS".equals(manifest.status)) {
            throw new IllegalStateException("Industrial loop guardian Entry workflow failed; see "
                    + manifest.entryLogPath);
        }
    }

    public static IndustrialRunManifest runPumpWear(Path outputDir, int ticks, long runOnceInMs) throws Exception {
        recreateDirectory(outputDir);
        Path layersDir = outputDir.resolve("layers");
        Files.createDirectories(layersDir);
        Path outputPath = outputDir.resolve("advisory-output.jsonl");
        Path summaryPath = outputDir.resolve("advisory-summary.json");
        Path contextPath = outputDir.resolve("context.json");
        Path entryLogPath = outputDir.resolve("entry.log");
        Path modelJar = outputDir.resolve("industrial-loop-guardian-model.jar");

        buildModelJar(modelJar);
        writeLayerMetadata(layersDir);
        String contextJson = writeContext(ticks, runOnceInMs, outputDir, layersDir, outputPath,
                summaryPath, contextPath, modelJar);

        IndustrialRunManifest manifest = new IndustrialRunManifest();
        manifest.mode = "local";
        manifest.modelJarPath = modelJar.toAbsolutePath().toString();
        manifest.contextClass = CONTEXT_CLASS;
        manifest.contextJsonPath = contextPath.toAbsolutePath().toString();
        manifest.layerMetaPath = layersDir.toAbsolutePath().toString();
        manifest.outputPath = outputPath.toAbsolutePath().toString();
        manifest.summaryPath = summaryPath.toAbsolutePath().toString();
        manifest.entryLogPath = entryLogPath.toAbsolutePath().toString();
        manifest.ticks = ticks;
        manifest.runOnceInMs = runOnceInMs;

        long started = System.nanoTime();
        manifest.exitCode = runEntryProcess(modelJar, contextJson, entryLogPath);
        manifest.wallClockElapsedMs = Math.round((System.nanoTime() - started) / 1_000_000.0);
        applyValidation(summaryPath, outputPath, manifest);
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(outputDir.resolve("manifest.json").toFile(), manifest);
        return manifest;
    }

    private static int runEntryProcess(Path modelJar, String contextJson, Path entryLogPath)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(javaBinary());
        command.add("-Xms32m");
        command.add("-Xmx256m");
        command.add("-XX:+UseSerialGC");
        command.add("-XX:MaxMetaspaceSize=128m");
        command.add("-XX:ReservedCodeCacheSize=32m");
        command.add("-Xss512k");
        command.add(ENTRY_CLASS);
        command.add("local");
        command.add(modelJar.toUri().toURL().toString());
        command.add(CONTEXT_CLASS);
        command.add(contextJson);

        Files.writeString(entryLogPath, String.join(System.lineSeparator(), command)
                + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.environment().put("CLASSPATH", launcherClasspath());
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(entryLogPath.toFile()));
        Process process = processBuilder.start();
        return process.waitFor();
    }

    private static void applyValidation(Path summaryPath, Path outputPath, IndustrialRunManifest manifest)
            throws IOException {
        manifest.outputRows = Files.exists(outputPath) ? Files.readAllLines(outputPath).size() : 0;
        manifest.behaviorAssertions.put("modeLocal", "local".equals(manifest.mode));
        manifest.behaviorAssertions.put("entryExited", manifest.exitCode == 0);
        manifest.behaviorAssertions.put("outputJsonlExists", Files.exists(outputPath));
        manifest.behaviorAssertions.put("summaryJsonExists", Files.exists(summaryPath));
        if (Files.exists(summaryPath)) {
            JsonNode summary = MAPPER.readTree(summaryPath.toFile());
            manifest.workflow = summary.path("workflow").asText();
            manifest.firstAdvisoryDelayMs = nullableLong(summary, "firstAdvisoryDelayMs");
            manifest.pumpWearFaultDetectionDelayMs = nullableLong(summary, "pumpWearFaultDetectionDelayMs");
            manifest.advisoryCount = summary.path("advisoryCount").asLong();
            manifest.faultAdvisoryCount = summary.path("faultAdvisoryCount").asLong();
            manifest.firstFaultAdvice = MAPPER.convertValue(summary.path("firstFaultAdvice"), Map.class);
            manifest.behaviorAssertions.put("faultDetected", "PASS".equals(summary.path("status").asText()));
            manifest.behaviorAssertions.put("advisoryOutputEvidence", manifest.firstFaultAdvice != null
                    && !manifest.firstFaultAdvice.isEmpty());
        } else {
            manifest.behaviorAssertions.put("faultDetected", false);
            manifest.behaviorAssertions.put("advisoryOutputEvidence", false);
        }
        manifest.status = manifest.exitCode == 0
                && manifest.behaviorAssertions.values().stream().allMatch(Boolean::booleanValue)
                ? "PASS" : "FAIL";
    }

    private static Long nullableLong(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asLong();
    }

    private static String writeContext(int ticks, long runOnceInMs, Path runDir, Path layersDir,
                                       Path outputPath, Path summaryPath, Path contextPath,
                                       Path modelJar) throws IOException {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("configuration.input.layermeta", layersDir.toAbsolutePath().toString());
        properties.put("configuration.neuronnet.classes", String.join(",", modelClasses()));
        properties.put("configuration.storage.json", storageJson(runDir));
        properties.put("configuration.history.slow.runs", "4");
        properties.put("configuration.history.fast.runs", "4");
        properties.put("configuration.slowfast.ratio", "1");
        properties.put("configuration.processing.frequency.map", frequencyMap());
        properties.put("configuration.input.inputs", inputsJson(ticks, runOnceInMs,
                runDir.resolve("input-audit.jsonl")));
        properties.put("configuration.isteacherstudying", "true");
        properties.put("configuration.maxRun", String.valueOf(ticks));
        properties.put("configuration.infiniteRun", "false");
        properties.put("configuration.outputAggregator", IndustrialLoopGuardianResultAggregator.class.getName());
        properties.put("configuration.runoncein", String.valueOf(runOnceInMs));
        properties.put("worker.threads.amount", "1");
        properties.put("configuration.discriminatorsAmount", "0");
        properties.put("configuration.demo.id", "industrial-loop-guardian-pump-wear");
        properties.put("configuration.demo.title", "Industrial loop guardian pump-wear Entry workflow");
        properties.put("configuration.demo.entry.mode", "local");
        properties.put("configuration.demo.modelJar.path", modelJar.toAbsolutePath().toString());
        properties.put("configuration.demo.output.path", outputPath.toAbsolutePath().toString());
        properties.put("configuration.demo.summary.path", summaryPath.toAbsolutePath().toString());
        properties.put("configuration.industrial.assetId", ASSET_ID);
        properties.put("configuration.industrial.faultStartTimestampMs",
                String.valueOf(FAULT_START_TICK * Math.max(1L, runOnceInMs)));
        properties.put("configuration.industrial.detection.anomaly.threshold", "0.60");
        properties.put("configuration.industrial.detection.fault.threshold", "0.55");

        ObjectNode root = MAPPER.createObjectNode();
        ObjectNode props = root.putObject("properties");
        properties.forEach(props::put);
        String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        Files.writeString(contextPath, json, StandardCharsets.UTF_8);
        return contextPath.toAbsolutePath().toString();
    }

    private static String inputsJson(int ticks, long runOnceInMs, Path inputAuditPath) throws IOException {
        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode inputData = root.putArray("inputData");
        ObjectNode item = inputData.addObject();
        ObjectNode source = item.putObject("iInputSource");
        source.put("clazz", IndustrialLoopGuardianReplayInput.class.getName());
        ObjectNode initInput = source.putObject("initInput");
        initInput.put("name", "pump-wear-iinitinput");
        initInput.put("assetId", ASSET_ID);
        initInput.put("ticks", ticks);
        initInput.put("cursor", 0);
        initInput.put("faultStartTick", FAULT_START_TICK);
        initInput.put("faultRiseTicks", FAULT_RISE_TICKS);
        initInput.put("runOnceInMs", runOnceInMs);
        initInput.put("epoch", 1);
        initInput.put("loop", 1);
        initInput.put("sampleRateHz", 4096.0);
        initInput.put("rotationalSpeedRpm", 1785.0);
        initInput.put("auditPath", inputAuditPath.toAbsolutePath().toString());
        item.put("mandatory", true);
        ObjectNode strategy = item.putObject("initStrategy");
        strategy.put("clazz", OneToAllFirstLayerInputStrategy.class.getName());
        strategy.putObject("iNeuronNetInput");
        item.put("amountOfRuns", ticks);
        return MAPPER.writeValueAsString(root);
    }

    private static String storageJson(Path rootPath) throws IOException {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("storageClass", DemoFileStorage.class.getName());
        ObjectNode storage = root.putObject("storage");
        storage.put("rootPath", rootPath.toAbsolutePath().toString());
        return MAPPER.writeValueAsString(root);
    }

    private static String frequencyMap() throws IOException {
        ObjectNode root = MAPPER.createObjectNode();
        for (String signalClass : List.of(
                MachineWaveformSignal.class.getName(),
                MachineFeatureSignal.class.getName(),
                FaultHypothesisSignal.class.getName(),
                MachineHealthAdvisorySignal.class.getName())) {
            ObjectNode frequency = root.putObject(signalClass);
            frequency.put("epoch", "1");
            frequency.put("loop", "1");
        }
        return MAPPER.writeValueAsString(root);
    }

    private static void writeLayerMetadata(Path layersDir) throws IOException {
        writeLayer(layersDir, 0, List.of(
                neuron(0, AcousticFeatureNeuron.class.getName(), MachineWaveformSignal.class.getName(),
                        AcousticFeatureProcessor.class.getName(), MachineFeatureSignal.class.getName(), 1, 0),
                neuron(1, VibrationFeatureNeuron.class.getName(), MachineWaveformSignal.class.getName(),
                        VibrationFeatureProcessor.class.getName(), MachineFeatureSignal.class.getName(), 1, 0)
        ));
        writeLayer(layersDir, 1, List.of(
                neuron(0, FaultHypothesisNeuron.class.getName(), MachineFeatureSignal.class.getName(),
                        FaultHypothesisProcessor.class.getName(), FaultHypothesisSignal.class.getName(), 2, 0)
        ));
        writeLayer(layersDir, 2, List.of(
                neuron(0, MachineHealthCorrelationNeuron.class.getName(), FaultHypothesisSignal.class.getName(),
                        MachineHealthCorrelationProcessor.class.getName(), MachineHealthAdvisorySignal.class.getName(), 3, 0)
        ));
        writeLayer(layersDir, 3, List.of(
                resultNeuron()
        ));
    }

    private static void writeLayer(Path layersDir, int layerId, List<ObjectNode> neurons) throws IOException {
        ObjectNode layer = MAPPER.createObjectNode();
        layer.put("layerID", layerId);
        layer.put("layerSize", neurons.size());
        ArrayNode neuronArray = layer.putArray("neurons");
        neurons.forEach(neuronArray::add);
        layer.set("metaParams", MAPPER.createObjectNode());
        Files.writeString(layersDir.resolve(String.valueOf(layerId)),
                MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(layer), StandardCharsets.UTF_8);
    }

    private static ObjectNode resultNeuron() {
        ObjectNode neuron = baseNeuron(0, MachineHealthResultNeuron.class.getName());
        ObjectNode processorMap = neuron.putObject("processorMap");
        ObjectNode processor = processorMap.putObject(MachineHealthAdvisorySignal.class.getName());
        processor.put("signalProcessorClass", MachineHealthAdvisoryGateProcessor.class.getName());
        neuron.set("mergerMap", MAPPER.createObjectNode());
        neuron.set("activationFunctions", MAPPER.createObjectNode());
        ObjectNode chain = neuron.putObject("signalChain");
        chain.put("signalChainClass", IndustrialSignalChain.class.getName());
        chain.putArray("signalClassNames").add(MachineHealthAdvisorySignal.class.getName());
        chain.put("description", "machine-health advisory gate");
        return neuron;
    }

    private static ObjectNode neuron(int neuronId, String neuronClass, String acceptedSignal,
                                     String processorClass, String outputSignal,
                                     int targetLayer, long targetNeuron) {
        ObjectNode neuron = baseNeuron(neuronId, neuronClass);
        if (AcousticFeatureNeuron.class.getName().equals(neuronClass)) {
            neuron.put("minimumNormalRms", 0.04);
            neuron.put("anomalyRmsMultiple", 2.25);
        } else if (VibrationFeatureNeuron.class.getName().equals(neuronClass)) {
            neuron.put("minimumNormalRms", 0.02);
            neuron.put("anomalyRmsMultiple", 1.80);
        } else if (MachineHealthCorrelationNeuron.class.getName().equals(neuronClass)) {
            neuron.put("modelVersion", "1.0.0-machine-health-entry");
        }

        ObjectNode processorMap = neuron.putObject("processorMap");
        ObjectNode processor = processorMap.putObject(acceptedSignal);
        processor.put("signalProcessorClass", processorClass);
        neuron.set("mergerMap", MAPPER.createObjectNode());
        neuron.set("activationFunctions", MAPPER.createObjectNode());
        neuron.set("axon", axonJson(neuronId, outputSignal, targetLayer, targetNeuron));
        ObjectNode chain = neuron.putObject("signalChain");
        chain.put("signalChainClass", IndustrialSignalChain.class.getName());
        chain.putArray("signalClassNames").add(acceptedSignal);
        chain.put("description", "industrial layer chain for " + acceptedSignal);
        return neuron;
    }

    private static ObjectNode baseNeuron(int neuronId, String neuronClass) {
        ObjectNode neuron = MAPPER.createObjectNode();
        neuron.put("neuronId", neuronId);
        neuron.put("currentNeuronClass", neuronClass);
        neuron.put("isProcessed", false);
        neuron.put("changed", false);
        neuron.put("onDelete", false);
        neuron.put("run", -1);
        return neuron;
    }

    private static ObjectNode axonJson(int sourceNeuron, String outputSignal, int targetLayer, long targetNeuron) {
        ObjectNode axon = MAPPER.createObjectNode();
        ObjectNode connectionMap = axon.putObject("connectionMap");
        ObjectNode addressMap = axon.putObject("addressMap");
        ObjectNode defaultWeights = axon.putObject("defaultWeights");
        axon.put("connectionsWrapped", false);
        ObjectNode defaultWeight = defaultWeights.putObject(outputSignal);
        defaultWeight.put("weightClass", IndustrialPassThroughWeight.class.getName());
        defaultWeight.put("signalClassName", outputSignal);
        ArrayNode connections = connectionMap.putArray(outputSignal);
        ObjectNode connection = connections.addObject();
        connection.put("targetLayerId", targetLayer);
        connection.put("sourceLayerId", targetLayer - 1);
        connection.put("targetNeuronId", targetNeuron);
        connection.put("sourceNeuronId", sourceNeuron);
        ObjectNode weight = connection.putObject("weight");
        weight.put("weightClass", IndustrialPassThroughWeight.class.getName());
        weight.put("signalClassName", outputSignal);
        connection.put("description", "industrial machine-health connection");
        addressMap.putObject(String.valueOf(targetLayer)).putArray(String.valueOf(targetNeuron));
        return axon;
    }

    private static List<String> modelClasses() {
        return List.of(
                IndustrialLoopGuardianReplayInput.class.getName(),
                IndustrialLoopGuardianResultAggregator.class.getName(),
                IndustrialSignalChain.class.getName(),
                IndustrialPassThroughWeight.class.getName(),
                MachineHealthResultNeuron.class.getName(),
                DemoFileStorage.class.getName(),
                DemoJsonContext.class.getName(),
                MachineWaveformSignal.class.getName(),
                MachineFeatureSignal.class.getName(),
                FaultHypothesisSignal.class.getName(),
                MachineHealthAdvisorySignal.class.getName(),
                IAcousticFeatureNeuron.class.getName(),
                IVibrationFeatureNeuron.class.getName(),
                IFaultHypothesisNeuron.class.getName(),
                IMachineHealthCorrelationNeuron.class.getName(),
                IAdvisoryGateNeuron.class.getName(),
                AcousticFeatureNeuron.class.getName(),
                VibrationFeatureNeuron.class.getName(),
                FaultHypothesisNeuron.class.getName(),
                MachineHealthCorrelationNeuron.class.getName(),
                AdvisoryGateNeuron.class.getName(),
                AcousticFeatureProcessor.class.getName(),
                VibrationFeatureProcessor.class.getName(),
                FaultHypothesisProcessor.class.getName(),
                MachineHealthCorrelationProcessor.class.getName(),
                MachineHealthAdvisoryGateProcessor.class.getName()
        );
    }

    private static void buildModelJar(Path modelJar) throws IOException, URISyntaxException {
        CodeSource codeSource = IndustrialLoopGuardianEntryLauncher.class.getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            throw new IllegalStateException("Cannot resolve worker code source for industrial model jar");
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
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(jarPath,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
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

    private static void recreateDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (var stream = Files.walk(directory)) {
                for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
        Files.createDirectories(directory);
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
        String executable = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")
                ? "java.exe" : "java";
        return javaHome.resolve("bin").resolve(executable).toString();
    }

    public static final class IndustrialRunManifest {
        public String status;
        public String mode;
        public String workflow;
        public String modelJarPath;
        public String contextClass;
        public String contextJsonPath;
        public String layerMetaPath;
        public String outputPath;
        public String summaryPath;
        public String entryLogPath;
        public int ticks;
        public long runOnceInMs;
        public int exitCode;
        public long wallClockElapsedMs;
        public int outputRows;
        public Long firstAdvisoryDelayMs;
        public Long pumpWearFaultDetectionDelayMs;
        public long advisoryCount;
        public long faultAdvisoryCount;
        public Map<String, Object> firstFaultAdvice = new LinkedHashMap<>();
        public Map<String, Boolean> behaviorAssertions = new LinkedHashMap<>();
    }
}
