package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import java.util.ArrayList;
import java.util.List;

public class UavSingleScenario {
    public String scenarioId;
    public UavOperatingMode mode = UavOperatingMode.FULLY_AUTONOMOUS;
    public long seed = 24703042047L;
    public int maxTicks = 40;
    public UavSingleConfig config = new UavSingleConfig();
    public SearchArea searchArea = new SearchArea();
    public UavPose initialPose = new UavPose(0.0, 0.0, 35.0, 0.0, 1.0, 1.0);
    public List<ObservationTarget> initialTargets = new ArrayList<>();
    public List<ScenarioTargetEvent> targetEvents = new ArrayList<>();
    public List<ScenarioConfirmationEvent> confirmationEvents = new ArrayList<>();
    public ScenarioFaults faults = new ScenarioFaults();

    public void validate() {
        if (scenarioId == null || scenarioId.isBlank()) {
            throw new IllegalArgumentException("scenarioId is required");
        }
        if (config == null) {
            config = new UavSingleConfig();
        }
        config.mode = mode;
        config.deterministicSeed = seed;
        config.validate();
        if (initialPose == null) {
            throw new IllegalArgumentException("initialPose is required");
        }
        if (searchArea == null) {
            searchArea = new SearchArea();
        }
        searchArea.validate(config);
        if (initialTargets == null) {
            initialTargets = new ArrayList<>();
        }
        if (targetEvents == null) {
            targetEvents = new ArrayList<>();
        }
        if (confirmationEvents == null) {
            confirmationEvents = new ArrayList<>();
        }
        if (faults == null) {
            faults = new ScenarioFaults();
        }
        for (ObservationTarget target : initialTargets) {
            validateTarget(target);
        }
        for (ScenarioTargetEvent event : targetEvents) {
            validateTarget(event.target);
        }
    }

    private static void validateTarget(ObservationTarget target) {
        if (target == null || target.targetId == null || target.targetId.isBlank()) {
            throw new IllegalArgumentException("every target requires targetId");
        }
    }
}
