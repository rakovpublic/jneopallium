package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AutonomousMindScenario {
    public String scenarioId;
    public String description;
    public long seed = 42L;
    public int maxTicks = 12;
    public AutonomousMindConfig config = new AutonomousMindConfig();
    public OwnerTask ownerTask;
    public List<String> map = new ArrayList<>();
    public double initialEnergy = 80.0;
    public boolean initiallyCharging;
    public boolean storedObservations;
    public boolean sensorConflict;
    public boolean radiationAnomaly;
    public boolean soundRadioAnomaly;
    public boolean unsafeOwnerTask;
    public boolean ambiguousTask;
    public boolean privacySensitiveRegion;
    public boolean emergencyFault;
    public Map<String, String> options = new LinkedHashMap<>();
}
