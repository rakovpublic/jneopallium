/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.lti;

import java.util.List;
import java.util.Objects;

/**
 * Runtime registration of one LMS platform against the tool
 * (14-LTI-XAPI.md §7).
 *
 * <p>Used by {@link LtiClientService} to verify LTI 1.3 launch JWTs:
 * {@code iss} must match {@link #issuer()}, {@code aud} must contain
 * {@link #clientId()}, the {@code lti_deployment_id} claim must be in
 * {@link #deploymentIds()}, and the signature is verified against keys
 * fetched from {@link #keysetUrl()}.
 */
public record LtiPlatformBinding(
        String issuer,
        String clientId,
        String authLoginUrl,
        String authTokenUrl,
        String keysetUrl,
        List<String> deploymentIds
) {

    public LtiPlatformBinding {
        Objects.requireNonNull(issuer, "issuer");
        Objects.requireNonNull(clientId, "clientId");
        deploymentIds = deploymentIds == null ? List.of() : List.copyOf(deploymentIds);
    }

    public static LtiPlatformBinding fromConfig(LtiBridgeConfig.PlatformConfig p) {
        return new LtiPlatformBinding(p.issuer(), p.clientId(),
                p.authLoginUrl(), p.authTokenUrl(),
                p.keysetUrl(), p.deploymentIds());
    }

    public boolean accepts(String issuerClaim, String audienceClaim, String deploymentIdClaim) {
        if (!issuer.equals(issuerClaim)) return false;
        if (audienceClaim == null || !audienceClaim.contains(clientId)) return false;
        if (deploymentIds.isEmpty()) return true;
        return deploymentIdClaim != null && deploymentIds.contains(deploymentIdClaim);
    }
}
