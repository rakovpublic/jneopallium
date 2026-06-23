/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.adfraud;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.demo.adfraud.AdFraudStreamingService;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.adfraud.AdFraudSignal;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdFraudModuleTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void eventTypeCoverage() {
        assertEquals(15, AdFraudEventType.values().length);
        assertEquals(7, AdFraudResponseAction.values().length);
    }

    @Test
    void signalsDeclareSemanticCadence() throws Exception {
        assertCadence("AdImpressionSignal", 1L, 1);
        assertCadence("AdClickSignal", 1L, 1);
        assertCadence("UserInteractionSignal", 2L, 1);
        assertCadence("SessionBehaviourSignal", 5L, 1);
        assertCadence("AttributionAnomalySignal", 5L, 1);
        assertCadence("GraphClusterSignal", 3L, 2);
        assertCadence("RetentionSignal", 10L, 2);
        assertCadence("EventIntegritySignal", 1L, 1);
        assertCadence("FraudDecisionSignal", 1L, 1);
    }

    @Test
    void deterministicIntegrityRulesFlagForgedAndReplay() {
        AdFraudRuntimeScorer scorer = new AdFraudRuntimeScorer(AdFraudModelBundle.rulesOnlyFallback());
        AdFraudEvent first = click("evt-1", 1_000L);
        first.signatureValid = false;
        first.nonce = "nonce-a";
        AdFraudDecision d1 = scorer.score(first);
        assertTrue(d1.getProbabilities().get("eventSpoofing") > 0.3);
        assertTrue(d1.getReasons().contains("signature invalid"));

        AdFraudEvent replay = click("evt-1", 2_000L);
        replay.nonce = "nonce-a";
        AdFraudDecision d2 = scorer.score(replay);
        assertTrue(d2.isDuplicateEvent());
        assertTrue(d2.getProbabilities().get("eventSpoofing") > d1.getProbabilities().get("eventSpoofing"));
    }

    @Test
    void sessionSequenceAndAttributionAnomaliesAreExplained() {
        AdFraudRuntimeScorer scorer = new AdFraudRuntimeScorer(AdFraudModelBundle.rulesOnlyFallback());
        AdFraudEvent install = click("install-1", 10_000L);
        install.eventType = AdFraudEventType.INSTALL;
        install.sessionEventCount = 55;
        AdFraudDecision decision = scorer.score(install);
        assertTrue(decision.getProbabilities().get("clickInjection") > 0.3);
        assertTrue(decision.getReasons().stream().anyMatch(r -> r.contains("conversion observed before click")));
    }

    @Test
    void baselineDoesNotLearnDuringStrongFraudEvidence() {
        PublisherBaselineNeuron neuron = new PublisherBaselineNeuron();
        neuron.observe("pub-1", 0.9, true);
        assertNull(neuron.baselineFor("pub-1"));
        assertEquals(1, neuron.getFrozenUpdates());
        neuron.observe("pub-1", 0.2, false);
        assertNotNull(neuron.baselineFor("pub-1"));
    }

    @Test
    void graphStateTtlEvictsOldEdges() {
        ClickFarmGraphNeuron graph = new ClickFarmGraphNeuron();
        graph.setGraphTtlMs(100L);
        graph.observeEdge("device-a", "account-1", 0L);
        graph.observeEdge("device-a", "account-2", 50L);
        assertTrue(graph.degree("device-a") >= 2);
        graph.evict(350L);
        assertEquals(0, graph.degree("device-a"));
    }

    @Test
    void responseGateIsAdvisoryOnly() {
        FraudResponseGateNeuron gate = new FraudResponseGateNeuron();
        gate.setRuntimeMode(AdFraudRuntimeMode.ADVISORY);
        assertEquals(AdFraudRuntimeMode.ADVISORY, gate.getRuntimeMode());
        assertFalse(gate.isAutomaticActionAllowed());
    }

    @Test
    void modelBundleLoadsAndVerifiesChecksumsAfterWorkflowExport() {
        AdFraudModelBundle bundle = AdFraudModelBundle.loadDefault();
        assertEquals("1.0", bundle.getSchemaVersion());
        assertFalse(bundle.getLabels().isEmpty());
        assertTrue(bundle.getFeatureNames().contains("integrity_risk"));
    }

    @Test
    void modelResourcesExposeSafetyCards() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(
                "model/advertising-fraud/model-descriptor.json")) {
            assertNotNull(in);
            JsonNode descriptor = MAPPER.readTree(in);
            assertEquals("advertising-fraud", descriptor.path("modelId").asText());
            assertFalse(descriptor.path("automatedActionReady").asBoolean());
            assertEquals(8, descriptor.path("totalLayers").asInt());
            assertEquals(22, descriptor.path("totalRealNeurons").asInt());
            assertTrue(descriptor.path("layers").isArray());
            assertEquals("layer-5-trained-fraud-correlation.json", descriptor.path("layers").get(5).path("file").asText());
            assertTrue(descriptor.path("signalFrequencyMap").has(
                    "com.rakovpublic.jneuropallium.worker.net.signals.impl.adfraud.FraudDecisionSignal"));
        }
    }

    @Test
    void modelLayerResourcesExposeFullNeuronDescriptions() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(
                "model/advertising-fraud/layer-5-trained-fraud-correlation.json")) {
            assertNotNull(in);
            JsonNode layer = MAPPER.readTree(in);
            assertEquals("trainedFraudCorrelation", layer.path("layerType").asText());
            assertEquals(1, layer.path("neurons").size());

            JsonNode neuron = layer.path("neurons").get(0);
            assertEquals("com.rakovpublic.jneuropallium.worker.net.neuron.impl.adfraud.FraudCorrelationNeuron",
                    neuron.path("currentNeuronClass").asText());
            assertTrue(neuron.path("processorMap").has(
                    "com.rakovpublic.jneuropallium.worker.net.signals.impl.adfraud.FraudEvidenceSignal"));
            assertTrue(neuron.path("dendrites").path("weights").has("integrity_risk"));
            assertTrue(neuron.path("signalChain").path("processingChain").isArray());
            assertEquals("fallback-model.json",
                    neuron.path("trainedAdvertisingFraudModel").path("snapshot").asText());
            assertTrue(neuron.path("trainedAdvertisingFraudModel").path("heads").has("bot"));
            assertTrue(neuron.path("trainedAdvertisingFraudModel").path("heads")
                    .path("bot").path("featureWeights").has("bot_risk"));
        }
    }

    @Test
    void processorsAreInterfaceTypedAndEmitDecisionSignals() throws Exception {
        ISignalProcessor processor = processor("EventAuthenticityProcessor");
        assertTrue(processor.getNeuronClass().isInterface());
        assertEquals(AdFraudSignal.class, processor.getSignalClass());
        AdFraudSignal input = new AdFraudSignal(click("evt-p", 1_000L));
        List<ISignal> out = processor.process(input, new AdFraudRuntimeScorer(AdFraudModelBundle.rulesOnlyFallback()));
        assertEquals(1, out.size());
        assertTrue(((AdFraudSignal) out.get(0)).getDecision().getOverallInvalidTrafficProbability() >= 0.0);
    }

    @Test
    void streamingServiceScoresAndExposesMetrics() throws Exception {
        try (AdFraudStreamingService service = new AdFraudStreamingService(new AdFraudRuntimeScorer(AdFraudModelBundle.rulesOnlyFallback()), 0)) {
            service.start();
            HttpClient client = HttpClient.newHttpClient();
            String body = MAPPER.writeValueAsString(click("svc-1", 1_000L));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:" + service.port() + "/v1/ad-fraud/score"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("overallInvalidTrafficProbability"));

            HttpResponse<String> metrics = client.send(HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:" + service.port() + "/metrics"))
                    .GET()
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertTrue(metrics.body().contains("events_total"));
            assertTrue(metrics.body().contains("fraud_probability_histogram"));
        }
    }

    private static ISignalProcessor processor(String simpleName) throws Exception {
        Class<?> clazz = Class.forName("com.rakovpublic.jneuropallium.worker.signalprocessor.impl.adfraud." + simpleName);
        Constructor<?> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        return (ISignalProcessor) constructor.newInstance();
    }

    private static void assertCadence(String simpleName, long epoch, int loop) throws Exception {
        Class<?> clazz = Class.forName("com.rakovpublic.jneuropallium.worker.net.signals.impl.adfraud." + simpleName);
        var field = clazz.getDeclaredField("PROCESSING_FREQUENCY");
        field.setAccessible(true);
        ProcessingFrequency frequency = (ProcessingFrequency) field.get(null);
        assertEquals(epoch, frequency.getEpoch(), simpleName + " epoch");
        assertEquals(loop, frequency.getLoop(), simpleName + " loop");
    }

    private static AdFraudEvent click(String id, long time) {
        AdFraudEvent e = new AdFraudEvent(id, AdFraudEventType.CLICK, time);
        e.ingestTime = time;
        e.sessionId = "s-" + id;
        e.publisherId = "pub-1";
        e.campaignId = "camp-1";
        e.deviceIdHash = "dev-1";
        e.nonce = "nonce-" + id;
        e.dwellMs = 40L;
        e.pointerEventCount = 1;
        e.pointerVelocityEntropy = 0.01;
        e.sourceTimestamp = time;
        e.serverReceiveTimestamp = time + 10;
        return e;
    }
}
