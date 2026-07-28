package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

public class AutonomousMindManifest {
    public String demoId = "demo-autonomous-mind";
    public String scenario;
    public String status;
    public String mode = "local";
    public String entrypoint = "com.rakovpublic.jneuropallium.worker.application.Entry";
    public String modelJarPath;
    public String contextClass = AutonomousMindContext.class.getName();
    public String contextJsonPath;
    public String layerMetadataPath;
    public Map<String, String> resultPaths = new LinkedHashMap<>();
    public int ticksRequested;
    public int ticksExecuted;
    public long seed;
    public Map<String, Boolean> acceptanceChecks = new LinkedHashMap<>();
    public Map<String, Object> metrics = new LinkedHashMap<>();
    public String summary;
    public int exitCode;
}
