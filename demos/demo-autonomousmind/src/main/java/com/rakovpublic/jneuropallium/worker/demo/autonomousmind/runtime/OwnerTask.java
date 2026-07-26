package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OwnerTask {
    public String taskId;
    public String ownerId;
    public String goal;
    public int priority;
    public Integer deadlineTicks;
    public List<String> allowedActions = new ArrayList<>();
    public List<String> forbiddenActions = new ArrayList<>();
    public List<String> requiredSensors = new ArrayList<>();
    public String targetZone;
    public Map<String, Object> successCriteria = new LinkedHashMap<>();
    public List<String> reportingRequirements = new ArrayList<>();
    public String maxRiskLevel = "LOW";
    public String energyPolicy = "PAUSE_AND_CHARGE_IF_NEEDED";
    public String clarificationPolicy = "ASK_OWNER_IF_AMBIGUOUS";
}
