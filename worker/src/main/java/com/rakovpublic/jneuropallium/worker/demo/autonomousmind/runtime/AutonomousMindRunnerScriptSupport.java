package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime;

import java.nio.file.Path;
import java.util.List;

public final class AutonomousMindRunnerScriptSupport {
    public static final String DEMO_ID = "demo-autonomous-mind-v1-video-game-ai";
    public static final String DEFAULT_SCENARIO = "baseline_foraging";
    public static final Path DEFAULT_OUTPUT_DIR = Path.of("target", "jneopallium-autonomous-mind");
    public static final String ENTRY_CLASS = "com.rakovpublic.jneuropallium.worker.application.Entry";

    private AutonomousMindRunnerScriptSupport() {
    }

    public static List<String> videoGameScenarios() {
        return AutonomousMindVideoGameSimulation.SCENARIOS;
    }
}
