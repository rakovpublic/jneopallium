package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import java.util.LinkedHashMap;
import java.util.Map;

public class UavSingleRunManifest {
    public String scenario;
    public UavOperatingMode mode;
    public String status;
    public int ticks;
    public boolean missionCompleted;
    public long deterministicSeed;
    public Map<String, String> artifacts = new LinkedHashMap<>();
    public Map<String, Object> metrics = new LinkedHashMap<>();
    public Map<String, Boolean> assertions = new LinkedHashMap<>();
}

