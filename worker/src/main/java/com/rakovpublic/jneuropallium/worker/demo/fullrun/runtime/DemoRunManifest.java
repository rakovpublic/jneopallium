package com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

public class DemoRunManifest {
    public String demoId;
    public String status;
    public String mode;
    public String modelJarPath;
    public String contextClass;
    public String contextJsonPath;
    public String layerMetaPath;
    public String outputPath;
    public String auditPath;
    public String entryLogPath;
    public int ticks;
    public long outputRows;
    public int exitCode;
    public Map<String, Boolean> behaviorAssertions = new LinkedHashMap<>();
    public Map<String, Object> metrics = new LinkedHashMap<>();
}
