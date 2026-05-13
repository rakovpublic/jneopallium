/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.fhir;

import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeBinding;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeBindingDirection;

import java.util.Objects;

/**
 * Resolved per-search FHIR binding (06-FHIR.md §7).
 *
 * <p>Every binding is {@link BridgeBindingDirection#READ} — the FHIR
 * bridge has no write direction (§3 rule 1). The clamp / ramp / loop
 * fields required by {@link BridgeBinding} are therefore {@code null};
 * they exist on the interface so the universal §2.2 audit path treats a
 * FHIR search uniformly with industrial bindings, but the FHIR bridge
 * never invokes them (no write code path exists).
 */
public record FhirSearchBinding(
        String bindingId,
        String fhirSearchTemplate,
        FhirBridgeConfig.TargetSignal targetSignal,
        String signalTag
) implements BridgeBinding {

    public FhirSearchBinding {
        Objects.requireNonNull(bindingId, "bindingId");
        Objects.requireNonNull(fhirSearchTemplate, "fhirSearchTemplate");
        Objects.requireNonNull(targetSignal, "targetSignal");
    }

    public static FhirSearchBinding fromConfig(FhirBridgeConfig.ReadBindingConfig r) {
        return new FhirSearchBinding(
                r.bindingId(),
                r.fhirSearch(),
                r.targetSignal(),
                r.signalTag());
    }

    @Override public BridgeBindingDirection direction() { return BridgeBindingDirection.READ; }
    @Override public String loopId() { return bindingId; }
    @Override public Double failSafeValue() { return null; }
    @Override public Double rampRateMaxPerSec() { return null; }
    @Override public Double minClampValue() { return null; }
    @Override public Double maxClampValue() { return null; }

    /**
     * Substitute {@code {pid}} in the configured search template with the
     * raw cohort patient id. The bridge resolves the search against the
     * raw id (the EHR only knows that id); pseudonymisation is applied
     * after the response is mapped to a signal.
     */
    public String resolveSearch(String rawPatientId) {
        if (rawPatientId == null) return fhirSearchTemplate;
        return fhirSearchTemplate.replace("{pid}", rawPatientId);
    }
}
