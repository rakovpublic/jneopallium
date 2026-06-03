package com.rakovpublic.jneuropallium.worker.demo.autonomousai.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

public class AutonomousAiRunManifest {
    public String demoName = "demo-autonomous-ai-gridworld";
    public String scenarioId;
    public String status;
    public String mode = "local";
    public String entrypoint = "com.rakovpublic.jneuropallium.worker.application.Entry";
    public String modelJarPath;
    public String contextClass = AutonomousAiDemoContext.class.getName();
    public String contextJsonPath;
    public String layerMetaPath;
    public String outputDir;
    public String resultsPath;
    public String transparencyPath;
    public String worldTracePath;
    public String safetySummaryPath;
    public String loopInterventionsPath;
    public String optionalLlmAdvisoryPath;
    public String entryLogPath;
    public long seed;
    public int ticks;
    public int exitCode;
    public int outputRows;
    public Map<String, Object> metrics = new LinkedHashMap<>();
    public Map<String, Boolean> behaviorAssertions = new LinkedHashMap<>();
}
