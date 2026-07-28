/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * LTI / xAPI bridge — Bridge 14 (14-LTI-XAPI.md).
 *
 * <p>Wires Jneopallium's tutoring signal pipeline into a learning
 * environment over the 1EdTech LTI 1.3 standard (course launch + identity
 * + AGS / NRPS services) and xAPI (Experience API) statements pulled from
 * or posted to a Learning Record Store.
 *
 * <ul>
 *   <li>{@link LtiClientService} parses and verifies LTI 1.3 launch JWTs
 *       and exposes the learner / course / line-item context.</li>
 *   <li>{@link XapiClientService} pulls xAPI statements from an LRS in
 *       PULL mode or accepts statements posted to the bridge in PUSH
 *       mode, decoding them into Jneopallium tutoring signals via
 *       {@link XapiStatementMapper}.</li>
 *   <li>{@link LtiAdvisoryOutputAggregator} emits xAPI
 *       {@code recommended} / {@code experienced} statements and
 *       (optionally) AGS {@code Score} proposals back to the LMS, with
 *       {@code gradingProgress=PendingManual} permanently locked in
 *       config — the loader rejects {@code FullyGraded}.</li>
 * </ul>
 *
 * <p><b>Bridge ceiling: ADVISORY.</b> The bridge cannot auto-grade,
 * auto-enrol, or modify a learner's record without instructor
 * confirmation:
 *
 * <ol>
 *   <li>{@link LtiBridgeConfigLoader} rejects {@code AUTONOMOUS} in
 *       {@code perTagSafetyMode} (14-LTI-XAPI.md §6, §9 S10).</li>
 *   <li>{@link LtiBridgeConfigLoader} rejects
 *       {@code gradingProgress: FullyGraded} on any AGS write binding
 *       (14-LTI-XAPI.md §5, §9 S10).</li>
 *   <li>{@link PseudonymService} pseudonymises every actor identifier
 *       before audit / advisory persistence (§9 S11, §10 R1).</li>
 *   <li>{@link XapiStatementMapper} drops {@code result.response}
 *       free-text by default (§9 S12, §10 R5).</li>
 * </ol>
 *
 * <p>Production wiring constructs a {@link JdkHttpXapiTransport}
 * (default) and a properly configured {@link LtiClientService.JwtVerifier}
 * (Nimbus / java-jwt JWKS backed). Acceptance tests use
 * {@link InMemoryXapiTransport}.
 */
package com.rakovpublic.jneuropallium.worker.bridge.lti;
