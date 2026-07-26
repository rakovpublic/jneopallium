/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.demo.industrialfmi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Path;

/** Immutable controller tuning for the skid demo. */
public record IndustrialFmiControllerConfig(
        double temperatureSetpoint,
        double minimumFlow,
        double highTemperatureTrip,
        double lowFlowTrip,
        double lowSuctionTrip,
        double coolingBase,
        double coolingKp,
        double coolingKi,
        double pumpBase,
        double pumpKp,
        double heaterBase,
        double heaterKp,
        double oscillationSeverityThreshold,
        double oscillationGainScale,
        double slowLoopPumpAdjustmentLimit,
        double fastLoopSeconds,
        double slowLoopSeconds
) {
    @JsonCreator
    public IndustrialFmiControllerConfig(
            @JsonProperty("temperatureSetpoint") Double temperatureSetpoint,
            @JsonProperty("minimumFlow") Double minimumFlow,
            @JsonProperty("highTemperatureTrip") Double highTemperatureTrip,
            @JsonProperty("lowFlowTrip") Double lowFlowTrip,
            @JsonProperty("lowSuctionTrip") Double lowSuctionTrip,
            @JsonProperty("coolingBase") Double coolingBase,
            @JsonProperty("coolingKp") Double coolingKp,
            @JsonProperty("coolingKi") Double coolingKi,
            @JsonProperty("pumpBase") Double pumpBase,
            @JsonProperty("pumpKp") Double pumpKp,
            @JsonProperty("heaterBase") Double heaterBase,
            @JsonProperty("heaterKp") Double heaterKp,
            @JsonProperty("oscillationSeverityThreshold") Double oscillationSeverityThreshold,
            @JsonProperty("oscillationGainScale") Double oscillationGainScale,
            @JsonProperty("slowLoopPumpAdjustmentLimit") Double slowLoopPumpAdjustmentLimit,
            @JsonProperty("fastLoopSeconds") Double fastLoopSeconds,
            @JsonProperty("slowLoopSeconds") Double slowLoopSeconds) {
        this(
                temperatureSetpoint == null ? 70.0 : temperatureSetpoint,
                minimumFlow == null ? 0.42 : minimumFlow,
                highTemperatureTrip == null ? 92.0 : highTemperatureTrip,
                lowFlowTrip == null ? 0.23 : lowFlowTrip,
                lowSuctionTrip == null ? 0.42 : lowSuctionTrip,
                coolingBase == null ? 35.0 : coolingBase,
                coolingKp == null ? 3.3 : coolingKp,
                coolingKi == null ? 0.18 : coolingKi,
                pumpBase == null ? 45.0 : pumpBase,
                pumpKp == null ? 95.0 : pumpKp,
                heaterBase == null ? 32.0 : heaterBase,
                heaterKp == null ? 4.0 : heaterKp,
                oscillationSeverityThreshold == null ? 0.62 : oscillationSeverityThreshold,
                oscillationGainScale == null ? 0.55 : oscillationGainScale,
                slowLoopPumpAdjustmentLimit == null ? 8.0 : slowLoopPumpAdjustmentLimit,
                fastLoopSeconds == null ? 0.1 : fastLoopSeconds,
                slowLoopSeconds == null ? 1.0 : slowLoopSeconds
        );
    }

    public static IndustrialFmiControllerConfig defaults() {
        return new IndustrialFmiControllerConfig(null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);
    }

    public static IndustrialFmiControllerConfig load(Path yaml) throws IOException {
        return new ObjectMapper(new YAMLFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
                .readValue(yaml.toFile(), IndustrialFmiControllerConfig.class);
    }
}
