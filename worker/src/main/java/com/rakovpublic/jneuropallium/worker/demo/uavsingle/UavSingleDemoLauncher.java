package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class UavSingleDemoLauncher {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static final List<String> VALID_SCENARIOS = List.of(
            "autonomous_success",
            "autonomous_priority_change",
            "confirm_approved",
            "confirm_denied",
            "confirm_timeout",
            "low_battery_rth",
            "geofence_veto",
            "lost_heartbeat",
            "poor_visibility",
            "duplicate_confirmation");
    public static final Path DEFAULT_OUTPUT_DIR = Path.of("target", "jneopallium-uav-single");

    private UavSingleDemoLauncher() {
    }

    public static void main(String[] args) throws Exception {
        Arguments arguments = Arguments.parse(args);
        List<UavSingleRunManifest> manifests = new ArrayList<>();
        if ("all".equals(arguments.scenarioId)) {
            for (String scenario : VALID_SCENARIOS) {
                manifests.add(runOne(scenario, arguments.outputDir));
            }
        } else {
            manifests.add(runOne(arguments.scenarioId, arguments.outputDir));
        }
        writeSummary(arguments.outputDir, manifests);
        printTable(manifests);
        boolean failed = manifests.stream().anyMatch(manifest -> !"PASS".equals(manifest.status));
        if (failed) {
            throw new IllegalStateException("UAV single demo failed; inspect " + arguments.outputDir.resolve("summary.json"));
        }
    }

    public static UavSingleRunManifest runOne(String scenarioId, Path outputRoot) throws Exception {
        if (!VALID_SCENARIOS.contains(scenarioId)) {
            throw new IllegalArgumentException("Unknown UAV single scenario: " + scenarioId);
        }
        Path scenarioPath = UavSingleScenarioLoader.resolveScenarioPath(scenarioId);
        UavSingleScenario scenario = UavSingleScenarioLoader.load(scenarioPath);
        Path outputDir = outputRoot.resolve(scenarioId);
        recreateDirectory(outputDir);
        UavSingleRunManifest manifest = new UavSingleSimulation(scenario, outputDir).run();
        manifest.artifacts.put("scenarioPath", scenarioPath.toAbsolutePath().toString());
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(outputDir.resolve("manifest.json").toFile(), manifest);
        return manifest;
    }

    private static void writeSummary(Path outputRoot, List<UavSingleRunManifest> manifests) throws IOException {
        Files.createDirectories(outputRoot);
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(outputRoot.resolve("summary.json").toFile(), manifests);
    }

    private static void printTable(List<UavSingleRunManifest> manifests) {
        System.out.println("scenario,status,mode,photosAccepted,safetyVetoes");
        for (UavSingleRunManifest manifest : manifests) {
            System.out.printf("%s,%s,%s,%s,%s%n",
                    manifest.scenario,
                    manifest.status,
                    manifest.mode,
                    manifest.metrics.get("photographsAccepted"),
                    manifest.metrics.get("safetyVetoes"));
        }
    }

    private static void recreateDirectory(Path dir) throws IOException {
        Path absolute = dir.toAbsolutePath().normalize();
        if (Files.exists(absolute) && !absolute.toString().contains("jneopallium-uav-single")) {
            throw new IllegalArgumentException("Refusing to recreate non-demo directory " + absolute);
        }
        if (Files.exists(absolute)) {
            try (var stream = Files.walk(absolute)) {
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
        Files.createDirectories(absolute);
    }

    private record Arguments(String scenarioId, Path outputDir) {
        static Arguments parse(String[] args) {
            String scenarioId = args.length == 0 ? "all" : args[0];
            Path output = DEFAULT_OUTPUT_DIR;
            for (int i = 1; i < args.length; i++) {
                if ("--output".equals(args[i]) && i + 1 < args.length) {
                    output = Path.of(args[++i]);
                }
            }
            return new Arguments(scenarioId, output);
        }
    }
}

